package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.events.ReadConfigFileEvent;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.resolver.AsyncDependencyResolver;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.sources.RecipeSources;
import io.reactivex.Observable;
import io.reactivex.Single;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.javatuples.Pair;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.loopperfect.buckaroo.MoreLists.concat;

public final class InstallTasks {

    private InstallTasks() {

    }

    private static Process<Event, ImmutableList<Dependency>> completeDependencies(
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


        final Process<Event, ReadConfigFileEvent> p = Process.of(CommonTasks.readAndMaybeGenerateConfigFile(projectDirectory.getFileSystem()));
        return p.chain(readConfigFileEvent -> {
            final BuckarooConfig config = readConfigFileEvent.config;
            final RecipeSource recipeSource = RecipeSources.standard(projectDirectory.getFileSystem(), config);
            final Single<ReadProjectFileEvent> projectFileEvent = CommonTasks.readProjectFile(projectFilePath);
            return Process.of(
                projectFileEvent
                    .cast(Event.class)
                    .toObservable(),
                projectFileEvent.map(event -> Pair.with(
                    event.project,
                    recipeSource
                )));
        }).chain(result -> {
            final RecipeSource recipeSource = result.getValue1();
            final Project project = result.getValue0();

            return completeDependencies(recipeSource, partialDependencies).chain(
                proposedDependencies -> Process.chain(
                    AsyncDependencyResolver.resolve(
                        recipeSource,
                        project.dependencies.add(proposedDependencies).entries()),

                    (ImmutableMap<RecipeIdentifier, Pair<SemanticVersion, ResolvedDependency>> deps) -> {
                        final DependencyLocks locks = DependencyLocks.of(
                            deps.entrySet().stream()
                                .map(i -> DependencyLock.of(i.getKey(), i.getValue().getValue1()))
                                .collect(toImmutableList())
                        );

                        return Process.of(Single.concat(
                            // Write the project file
                            CommonTasks.writeFile(
                                Serializers.serialize(project.addDependencies(proposedDependencies)),
                                projectFilePath,
                                true),

                            // Write the lock file
                            CommonTasks.writeFile(
                                Serializers.serialize(locks),
                                lockFilePath,
                                true))
                                .toObservable()
                                .cast(Event.class),
                            Single.fromCallable(()->Notification.of("install complete"))

                        );

                    }

                ));
        }).chain(x->
            Process.of(
                InstallExistingTasks.installExistingDependencies(projectDirectory),
                Single.just(x)
            )
        ).states();
    }

    public static Observable<Event> installDependencyInWorkingDirectory(final FileSystem fs, final ImmutableList<PartialDependency> partialDependencies) {
        Preconditions.checkNotNull(fs);
        return installDependency(fs.getPath(""), partialDependencies);
    }
}

