package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.events.ReadConfigFileEvent;
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

    public static Observable<Event> resolveDependencies(final Path projectDirectory) {

        Preconditions.checkNotNull(projectDirectory);

        final Path projectFilePath = projectDirectory.resolve("buckaroo.json").toAbsolutePath();

        final Process<Event, ReadConfigFileEvent> p = Process.of(
            Observable.just((Event)Notification.of("Resolving dependencies... ")),
            CommonTasks.readAndMaybeGenerateConfigFile(projectDirectory.getFileSystem()));

        return p.chain(config -> {

            final Process<Event, Project> p2 = CommonTasks.readProjectFile(projectFilePath);

            return p2.chain((Project project) -> {
                final RecipeSource recipeSource = RecipeSources.standard(projectDirectory.getFileSystem(), config.config);

                return AsyncDependencyResolver.resolve(
                    recipeSource, project.dependencies.entries()).map(ResolvedDependenciesEvent::of);

            }).map(i -> DependencyLocks.of(i.dependencies)).chain((DependencyLocks dependencyLocks) -> {

                final Path lockFilePath = projectDirectory.resolve("buckaroo.lock.json").toAbsolutePath();

                return Process.usingLastAsResult(
                    CommonTasks.writeFile(Serializers.serialize(dependencyLocks), lockFilePath, true)
                        .toObservable()
                        .cast(Event.class)
                        .concatWith(Observable.just((Event)Notification.of("Resolving dependencies complete"))));
            });

        }).states();
    }

    public static Observable<Event> resolveDependenciesInWorkingDirectory(final FileSystem fs) {
        Preconditions.checkNotNull(fs);
        return resolveDependencies(fs.getPath(""));
    }
}
