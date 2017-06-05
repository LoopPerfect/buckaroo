package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
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

    private static Single<ImmutableList<Dependency>> completeDependencies(
        final RecipeSource recipeSource, final ImmutableList<PartialDependency> partialDependencies) {

        Preconditions.checkNotNull(recipeSource);
        Preconditions.checkNotNull(partialDependencies);

        return Single.concat(
            partialDependencies.stream()
                .map(x -> RecipeSources.resolve(recipeSource, x).singleOrError())
                .collect(toImmutableList()))
            .toList()
            .map(x -> x.stream()
                .distinct()
                .collect(toImmutableList()));
    }

    public static Observable<Event> installDependencyInWorkingDirectory(final FileSystem fs, final ImmutableList<PartialDependency> partialDependencies) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(partialDependencies);

        final Path projectFilePath = fs.getPath("buckaroo.json").toAbsolutePath();
        final Path lockFilePath = fs.getPath("buckaroo.lock.json").toAbsolutePath();

        return Observable.concat(

            // First, add the dependency to project file and regenerate the lock.
            MoreSingles.chainObservable(

                // Read the config file
                CommonTasks.readAndMaybeGenerateConfigFile(fs),

                (ReadConfigFileEvent readConfigFileEvent) -> {

                    final BuckarooConfig config = readConfigFileEvent.config;
                    final RecipeSource recipeSource = RecipeSources.standard(fs, config);

                    return MoreSingles.chainObservable(

                        // Read the project file
                        CommonTasks.readProjectFile(projectFilePath),

                        (ReadProjectFileEvent readProjectFileEvent) -> {

                            final Project project = readProjectFileEvent.project;

                            // Complete the partial dependencies
                            return completeDependencies(recipeSource, partialDependencies)
                                .flatMapObservable(proposedDependencies -> MoreSingles.chainObservable(

                                    // Resolve the dependencies
                                    AsyncDependencyResolver.resolve(
                                        recipeSource,
                                        project.dependencies.add(proposedDependencies).entries())
                                        .map(ResolvedDependenciesEvent::of2),

                                    // Write the project and lock files
                                    (ResolvedDependenciesEvent resolvedDependenciesEvent) -> {

                                        final DependencyLocks locks = DependencyLocks.of(
                                            resolvedDependenciesEvent.dependencies.entrySet()
                                                .stream()
                                                .map(i -> DependencyLock.of(i.getKey(), i.getValue()))
                                                .collect(toImmutableList()));

                                        return Single.concat(

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

                                            .cast(Event.class)
                                            .toObservable();
                                    }));
                        }
                );
            }),

            // Next, run the install routine!
            InstallExistingTasks.installExistingDependenciesInWorkingDirectory(fs)
        );
    }
}

