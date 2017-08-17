package com.loopperfect.buckaroo.github;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.resolver.AsyncDependencyResolver;
import com.loopperfect.buckaroo.sources.GitProviderRecipeSource;
import com.loopperfect.buckaroo.sources.RecipeFetchException;
import com.loopperfect.buckaroo.tasks.InitTasks;
import com.loopperfect.buckaroo.tasks.InstallTasks;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class GitHubGitProviderTest {

    @Test
    public void fetch() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(fs, GitHubGitProvider.of());

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-c"))
            .result();

        task.timeout(90, TimeUnit.SECONDS).blockingGet();
    }

    @Test
    public void fetchFailsWhenThereAreNoReleases() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(fs, GitHubGitProvider.of());

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-no-releases"))
            .result();

        task.timeout(90, TimeUnit.SECONDS).subscribe(
            result -> {
                assertTrue(false);
            }, error -> {
                assertTrue(error instanceof RecipeFetchException);
            });
    }

    @Test
    public void fetchFailsWhenThereIsNoProjectFile() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(fs, GitHubGitProvider.of());

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-no-project"))
            .result();

        task.timeout(90, TimeUnit.SECONDS).subscribe(
            result -> {
                assertTrue(false);
            }, error -> {
                assertTrue(error instanceof RecipeFetchException);
            });
    }

    @Test
    public void fetchGivesProgressEvents() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(fs, GitHubGitProvider.of());

        final Observable<Event> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-c")).states();

        final List<Event> actual = task.timeout(90, TimeUnit.SECONDS).toList().blockingGet();

        assertTrue(actual.size() > 1);
    }

    @Test
    public void fetchRecognizesTarget() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(fs, GitHubGitProvider.of());

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-d"))
            .result();

        final Optional<String> expected = Optional.of("target-d");

        final Optional<String> actual = task.timeout(90, TimeUnit.SECONDS).blockingGet()
            .versions.get(SemanticVersion.of(0, 1)).target;

        assertEquals(expected, actual);
    }

    @Test
    public void resolve1() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(fs, GitHubGitProvider.of());

        final ImmutableList<Dependency> dependencies = ImmutableList.of(
            Dependency.of(
                RecipeIdentifier.of("github", "njlr", "test-lib-d"),
                AnySemanticVersion.of()));

        final ResolvedDependencies result = AsyncDependencyResolver.resolve(recipeSource, dependencies)
            .result()
            .timeout(20000L, TimeUnit.MILLISECONDS)
            .blockingGet();

        assertTrue(result.isComplete());
        assertTrue(result.dependencies.size() == 1);
        assertTrue(result.dependencies.containsKey(RecipeIdentifier.of("github", "njlr", "test-lib-d")));
    }

    @Test
    public void resolve2() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(fs, GitHubGitProvider.of());

        final ImmutableList<Dependency> dependencies = ImmutableList.of(
            Dependency.of(
                RecipeIdentifier.of("github", "njlr", "test-lib-e"),
                AnySemanticVersion.of()));

        final ResolvedDependencies result = AsyncDependencyResolver.resolve(recipeSource, dependencies)
            .result()
            .timeout(20000L, TimeUnit.MILLISECONDS)
            .blockingGet();

        assertTrue(result.isComplete());
        assertTrue(result.dependencies.size() == 1);
        assertTrue(result.dependencies.containsKey(RecipeIdentifier.of("github", "njlr", "test-lib-e")));
    }
}