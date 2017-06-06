package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.resolver.AsyncDependencyResolver;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.sources.RecipeSources;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.nio.file.Path;

public final class ResolveTasks {

    private ResolveTasks() {

    }

    public static Observable<Event> resolveDependenciesInWorkingDirectory(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        final Path projectFilePath = fs.getPath("buckaroo.json").toAbsolutePath();

        return CommonTasks.readAndMaybeGenerateConfigFile(fs).flatMapObservable(config -> MoreObservables.chain(

            // Read the project file
            CommonTasks.readProjectFile(projectFilePath)
                    .toObservable(),

            // Resolve the dependencies
            (ReadProjectFileEvent event) ->  {

                final RecipeSource recipeSource = RecipeSources.standard(fs, config.config);

                return AsyncDependencyResolver.resolve(
                    recipeSource, event.project.dependencies.entries())
                    .map(ResolvedDependenciesEvent::of2)
                    .toObservable();
            },

            // Write the lock file
            (ResolvedDependenciesEvent event) -> {

                final DependencyLocks locks = DependencyLocks.of(event.dependencies.entrySet()
                    .stream()
                    .map(x -> DependencyLock.of(x.getKey(), x.getValue()))
                    .collect(ImmutableList.toImmutableList()));

                final Path lockFilePath = fs.getPath("buckaroo.lock.json").toAbsolutePath();

                return CommonTasks.writeFile(Serializers.serialize(locks), lockFilePath, true)
                    .toObservable()
                    .cast(Event.class);
            })
        );
    }
}
