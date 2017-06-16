package com.loopperfect.buckaroo.tasks;

import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Jimfs;
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

        final FileSystem fs = Jimfs.newFileSystem();
        final Scheduler scheduler = Schedulers.newThread();

        final Project project = Project.of();

        EvenMoreFiles.writeFile(
            fs.getPath("buckaroo.json"),
            Serializers.serialize(project));

        final Observable<Event> task = ResolveTasks.resolveDependenciesInWorkingDirectory(Context.of(fs, scheduler));

        task.toList().blockingGet();

        assertEquals(
            right(DependencyLocks.of()),
            Serializers.parseDependencyLocks(EvenMoreFiles.read(fs.getPath("buckaroo.lock.json"))));
    }

    @Test
    public void simpleProject() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();
        final Scheduler scheduler = Schedulers.newThread();

        final Recipe recipe = Recipe.of(
            "example",
            "https://github.com/org/example",
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/org/example/commit", "c7355d5"),
                    "example")));

        EvenMoreFiles.writeFile(fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "org", "example.json"),
            Serializers.serialize(recipe));

        final Project project = Project.of(
            "Example",
            DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "example"), AnySemanticVersion.of())));

        EvenMoreFiles.writeFile(
            fs.getPath("buckaroo.json"),
            Serializers.serialize(project));

        final Observable<Event> task = ResolveTasks.resolveDependenciesInWorkingDirectory(Context.of(fs, scheduler));

        task.toList().blockingGet();

        final DependencyLocks expected = DependencyLocks.of(DependencyLock.of(
            RecipeIdentifier.of("org", "example"),
            ResolvedDependency.from(recipe.versions.get(SemanticVersion.of(1)))));

        assertEquals(
            right(expected),
            Serializers.parseDependencyLocks(EvenMoreFiles.read(fs.getPath("buckaroo.lock.json"))));
    }
}
