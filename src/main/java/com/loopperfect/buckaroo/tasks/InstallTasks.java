package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.events.ReadConfigFileEvent;
import com.loopperfect.buckaroo.resolver.AsyncDependencyResolver;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.sources.RecipeSources;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class InstallTasks {

    private InstallTasks() {

    }

    public static Process<Event, ImmutableList<Dependency>> completeDependencies(
        final RecipeSource recipeSource, final ImmutableList<PartialDependency> partialDependencies) {

        Preconditions.checkNotNull(recipeSource);
        Preconditions.checkNotNull(partialDependencies);

        final ImmutableList<Process<Event, Dependency>> resolveProcesses = partialDependencies.stream()
            .map(x -> RecipeSources.resolve(recipeSource, x))
            .collect(toImmutableList());

//        final Observable<Event> states = Observable
//            .merge(resolveProcesses.stream().map(x -> x.states()).collect(toImmutableList()));

//        final Observable<Event> states = Observable.empty(); // TODO: Include the states somehow.

//        final Single<ImmutableList<Dependency>> result = resolveProcesses.stream()
//            .map(p -> p.result().map(ImmutableList::of))
//            .reduce(Single.just(ImmutableList.of()), (x, y) -> x.flatMap(a -> y.map(b -> concat(a, b))));

        return Process.merge(resolveProcesses);
    }

    public static Observable<Event> installDependency(
        final Path projectDirectory, final ImmutableList<PartialDependency> partialDependencies) {

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
                    CommonTasks.readProjectFile(projectFilePath),

                    (Project project) -> {

                        return Process.chain(

                            completeDependencies(recipeSource, partialDependencies),

                            // Use the resolver to fill in partial dependencies
                            (ImmutableList<Dependency> proposedDependencies) -> Process.chain(

                                AsyncDependencyResolver.resolve(
                                    recipeSource,
                                    project.dependencies.add(proposedDependencies).entries())
                                    .map(DependencyLocks::of),

                                (DependencyLocks locks) -> Process.chain(

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
                                )
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

