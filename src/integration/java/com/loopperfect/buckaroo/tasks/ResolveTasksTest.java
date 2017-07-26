package com.loopperfect.buckaroo.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import io.reactivex.Observable;
import org.eclipse.jgit.api.Git;
import org.javatuples.Pair;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Optional;

import static com.loopperfect.buckaroo.Either.left;
import static com.loopperfect.buckaroo.Either.right;
import static org.junit.Assert.assertEquals;

public final class ResolveTasksTest {

    @Test
    public void generateDependencyLocksEmpty() throws Exception {

        final Project project = Project.of(
            Optional.of("my-project"),
            Optional.empty(),
            Optional.empty(),
            DependencyGroup.of());

        final ResolvedDependencies resolvedDependencies = ResolvedDependencies.of();

        final DependencyLocks dependencyLocks = ResolveTasks.generateDependencyLocksFromProjectAndResolvedDependencies(
            project, resolvedDependencies);

        assertEquals(DependencyLocks.of(), dependencyLocks);
    }

    @Test
    public void generateDependencyLocks1() throws Exception {

        final Project project = Project.of(
            Optional.of("my-project"),
            Optional.empty(),
            Optional.empty(),
            DependencyGroup.of(
                Dependency.of(
                    RecipeIdentifier.of("org", "example"),
                    AnySemanticVersion.of())));

        final ResolvedDependencies resolvedDependencies = ResolvedDependencies.of(
            ImmutableMap.of(
                RecipeIdentifier.of("org", "example"),
                Pair.with(
                    SemanticVersion.of(1),
                    RecipeVersion.of(GitCommit.of("https://github.com/org/example/commit", "c7355d5")))));

        final DependencyLocks dependencyLocks = ResolveTasks.generateDependencyLocksFromProjectAndResolvedDependencies(
            project, resolvedDependencies);

        final DependencyLocks expected = DependencyLocks.of(
            DependencyLock.of(
                RecipeIdentifier.of("org", "example"),
                ResolvedDependency.of(left(GitCommit.of("https://github.com/org/example/commit", "c7355d5")))));

        assertEquals(expected, dependencyLocks);
    }

    @Test
    public void generateDependencyLocks2() throws Exception {

        final Project project = Project.of(
            Optional.of("my-project"),
            Optional.empty(),
            Optional.empty(),
            DependencyGroup.of(
                Dependency.of(
                    RecipeIdentifier.of("org", "example"),
                    AnySemanticVersion.of())),
            PlatformDependencyGroup.of(
                Pair.with(
                    "^linux.*",
                    DependencyGroup.of(
                        Dependency.of(
                            RecipeIdentifier.of("org", "linux-only"),
                            AnySemanticVersion.of())))));

        final ResolvedDependencies resolvedDependencies = ResolvedDependencies.of(
            ImmutableMap.of(
                RecipeIdentifier.of("org", "example"),
                Pair.with(
                    SemanticVersion.of(1),
                    RecipeVersion.of(GitCommit.of("https://github.com/org/example/commit", "c7355d5"))),
                RecipeIdentifier.of("org", "linux-only"),
                Pair.with(
                    SemanticVersion.of(1),
                    RecipeVersion.of(GitCommit.of("https://github.com/org/linux-only/commit", "b8945e7")))));

        final DependencyLocks dependencyLocks = ResolveTasks.generateDependencyLocksFromProjectAndResolvedDependencies(
            project, resolvedDependencies);

        final DependencyLocks expected = DependencyLocks.of(
            DependencyLock.of(
                RecipeIdentifier.of("org", "example"),
                ResolvedDependency.of(left(GitCommit.of("https://github.com/org/example/commit", "c7355d5")))));

        assertEquals(expected, dependencyLocks);
    }

    @Test
    public void emptyProject() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Project project = Project.of();

        EvenMoreFiles.writeFile(
            fs.getPath("buckaroo.json"),
            Serializers.serialize(project));

        final Observable<Event> task = ResolveTasks.resolveDependenciesInWorkingDirectory(fs);

        task.toList().blockingGet();

        assertEquals(
            right(DependencyLocks.of()),
            Serializers.parseDependencyLocks(EvenMoreFiles.read(fs.getPath("buckaroo.lock.json"))));
    }

    @Test
    public void simpleProject() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Recipe recipe = Recipe.of(
            "example",
            "https://github.com/org/example",
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/org/example/commit", "c7355d5"))));

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

        final Observable<Event> task = ResolveTasks.resolveDependenciesInWorkingDirectory(fs);

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
            Serializers.parseDependencyLocks(EvenMoreFiles.read(fs.getPath("buckaroo.lock.json"))));
    }

    @Test
    public void projectWithTarget() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Recipe recipe = Recipe.of(
            "example",
            "https://github.com/org/example",
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/org/example/commit", "c7355d5"),
                    "some-custom-target")));

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

        final Observable<Event> task = ResolveTasks.resolveDependenciesInWorkingDirectory(fs);

        task.toList().blockingGet();

        final DependencyLocks expected = DependencyLocks.of(DependencyLock.of(
            RecipeIdentifier.of("org", "example"),
            ResolvedDependency.of(
                recipe.versions.get(SemanticVersion.of(1)).source,
                recipe.versions.get(SemanticVersion.of(1)).target,
                recipe.versions.get(SemanticVersion.of(1)).buckResource,
                ImmutableList.of())));

        final Either<JsonParseException, DependencyLocks> actual = Serializers.parseDependencyLocks(
            EvenMoreFiles.read(fs.getPath("buckaroo.lock.json")));

        assertEquals(right(expected), actual);
    }
}
