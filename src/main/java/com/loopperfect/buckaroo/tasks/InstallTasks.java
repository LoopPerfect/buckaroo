package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.events.ReadConfigFileEvent;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.resolver.AsyncDependencyResolver;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.sources.RecipeSources;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.loopperfect.buckaroo.MoreLists.concat;

public final class InstallTasks {

    private InstallTasks() {

    }

    static Process<Event, ImmutableList<Dependency>> completeDependencies(
        final RecipeSource recipeSource, final ImmutableList<PartialDependency> partialDependencies) {

        Preconditions.checkNotNull(recipeSource);
        Preconditions.checkNotNull(partialDependencies);

        final ImmutableList<Process<Event, Dependency>> ps =
            partialDependencies.stream()
                .map(x -> RecipeSources.resolve(recipeSource, x)).collect(toImmutableList());

        final Single<ImmutableList<Dependency>> deps = ps.stream()
            .map(p -> p.result().map(ImmutableList::of))
            .reduce((x, y) -> x.flatMap(a -> y.map(b -> concat(a, b))))
            .orElseGet(() -> Single.just(ImmutableList.of()));

        final Observable<Event> os = Observable
            .merge(ps.stream().map(Process::states).collect(toImmutableList()));

        return Process.of(os, deps);
    }

    public static Observable<Event> installDependency(final Path projectDirectory, final ImmutableList<PartialDependency> partialDependencies) {

        Preconditions.checkNotNull(projectDirectory);
        Preconditions.checkNotNull(partialDependencies);

        final Path projectFilePath = projectDirectory.resolve("buckaroo.json").toAbsolutePath();
        final Path lockFilePath = projectDirectory.resolve("buckaroo.lock.json").toAbsolutePath();

        return Process.chain(

            // Read the config file
            Process.of(CommonTasks.readAndMaybeGenerateConfigFile(projectDirectory.getFileSystem())),

            (ReadConfigFileEvent readConfigFileEvent) -> {

                final BuckarooConfig config = readConfigFileEvent.config;
                final RecipeSource recipeSource = RecipeSources.standard(projectDirectory.getFileSystem(), config);

                return Process.chain(

                    // Read the project file
                    Process.of(CommonTasks.readProjectFile(projectFilePath)),

                    (ReadProjectFileEvent readProjectFileEvent) -> {

                        final Project project = readProjectFileEvent.project;

                        return Process.chain(

                            completeDependencies(recipeSource, partialDependencies),

                            // Use the resolver to fill in partial dependencies
                            (ImmutableList<Dependency> proposedDependencies) -> Process.chain(

                                AsyncDependencyResolver.resolve(
                                    recipeSource,
                                    project.dependencies.add(proposedDependencies).entries()),

                                (ResolvedDependencies resolvedDependencies) -> {

                                    final DependencyLocks locks = DependencyLocks.of(
                                        resolvedDependencies.dependencies.entrySet().stream()
                                            .map(i -> DependencyLock.of(i.getKey(), i.getValue().getValue1()))
                                            .collect(toImmutableList()));

                                    return Process.chain(

                                        // Write the project file
                                        Process.of(
                                            CommonTasks.writeFile(
                                                Serializers.serialize(project.addDependencies(proposedDependencies)),
                                                projectFilePath,
                                                true)),

                                        // Write the lock file
                                        ignored -> Process.of(CommonTasks.writeFile(
                                            Serializers.serialize(locks),
                                            lockFilePath,
                                            true)),

                                        // Send a notification
                                        ignored -> Process.of(
                                            InstallExistingTasks.installExistingDependencies(projectDirectory),
                                            Single.just(Notification.of("Installation complete. ")))
                                    );
                                }
                            )
                        );
                    });
            }).states();
    }

    public static Observable<Event> installDependencyInWorkingDirectory(final FileSystem fs, final ImmutableList<PartialDependency> partialDependencies) {
        Preconditions.checkNotNull(fs);
        return installDependency(fs.getPath(""), partialDependencies);
    }
}

