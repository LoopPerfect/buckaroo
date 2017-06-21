package com.loopperfect.buckaroo.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.nio.file.FileSystem;

import static com.loopperfect.buckaroo.Either.right;
import static org.junit.Assert.assertEquals;

public final class ResolveTasksTest {

    @Test
    public void emptyProject() throws Exception {

        final Context context = Context.of(
            Jimfs.newFileSystem(),
            Schedulers.newThread());

        final Project project = Project.of();

        EvenMoreFiles.writeFile(
            context.fs.getPath("buckaroo.json"),
            Serializers.serialize(project));

        final Observable<Event> task = ResolveTasks.resolveDependenciesInWorkingDirectory(context);

        task.toList().blockingGet();

        assertEquals(
            right(DependencyLocks.of()),
            Serializers.parseDependencyLocks(EvenMoreFiles.read(context.fs.getPath("buckaroo.lock.json"))));
    }

    @Test
    public void simpleProject() throws Exception {

        final Context context = Context.of(
            Jimfs.newFileSystem(),
            Schedulers.newThread());

        final Recipe recipe = Recipe.of(
            "example",
            "https://github.com/org/example",
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/org/example/commit", "c7355d5"))));

        EvenMoreFiles.writeFile(context.fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "org", "example.json"),
            Serializers.serialize(recipe));

        final Project project = Project.of(
            "Example",
            DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "example"), AnySemanticVersion.of())));

        EvenMoreFiles.writeFile(
            context.fs.getPath("buckaroo.json"),
            Serializers.serialize(project));

        final Observable<Event> task = ResolveTasks.resolveDependenciesInWorkingDirectory(context);

        task.toList().blockingGet();

        final DependencyLocks expected = DependencyLocks.of(DependencyLock.of(
            RecipeIdentifier.of("org", "example"),
            ResolvedDependency.of(
                recipe.versions.get(SemanticVersion.of(1)).source,
                recipe.versions.get(SemanticVersion.of(1)).target,
                recipe.versions.get(SemanticVersion.of(1)).buckResource,
                ImmutableList.of())));

        assertEquals(
            right(expected),
            Serializers.parseDependencyLocks(EvenMoreFiles.read(context.fs.getPath("buckaroo.lock.json"))));
    }

    @Test
    public void projectWithTarget() throws Exception {

        final Context context = Context.of(
            Jimfs.newFileSystem(),
            Schedulers.newThread());

        final Recipe recipe = Recipe.of(
            "example",
            "https://github.com/org/example",
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/org/example/commit", "c7355d5"),
                    "some-custom-target")));

        EvenMoreFiles.writeFile(context.fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "org", "example.json"),
            Serializers.serialize(recipe));

        final Project project = Project.of(
            "Example",
            DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "example"), AnySemanticVersion.of())));

        EvenMoreFiles.writeFile(
            context.fs.getPath("buckaroo.json"),
            Serializers.serialize(project));

        final Observable<Event> task = ResolveTasks.resolveDependenciesInWorkingDirectory(context);

        task.toList().blockingGet();

        final DependencyLocks expected = DependencyLocks.of(DependencyLock.of(
            RecipeIdentifier.of("org", "example"),
            ResolvedDependency.of(
                recipe.versions.get(SemanticVersion.of(1)).source,
                recipe.versions.get(SemanticVersion.of(1)).target,
                recipe.versions.get(SemanticVersion.of(1)).buckResource,
                ImmutableList.of())));

        final Either<JsonParseException, DependencyLocks> actual = Serializers.parseDependencyLocks(
            EvenMoreFiles.read(context.fs.getPath("buckaroo.lock.json")));

        assertEquals(right(expected), actual);
    }
}
