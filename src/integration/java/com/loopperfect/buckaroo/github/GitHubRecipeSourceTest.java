package com.loopperfect.buckaroo.github;

import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class GitHubRecipeSourceTest {

    @Test
    public void fetchTags() throws Exception {

        final ImmutableMap<String, GitCommitHash> expected = ImmutableMap.of(
            "v2", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"),
            "v0.1.0", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"),
            "v1.0.0-rc1", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"));

        final ImmutableMap<String, GitCommitHash> actual = GitHub.fetchTags(
            Identifier.of("njlr"), Identifier.of("test-lib-tags"));

        assertEquals(expected, actual);
    }

    @Test
    public void fetch() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitHubRecipeSource.of(fs);

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-c"))
            .result();

        task.timeout(90, TimeUnit.SECONDS).blockingGet();
    }

    @Test
    public void fetchFailsWhenThereAreNoReleases() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitHubRecipeSource.of(fs);

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-no-releases"))
            .result();

        task.timeout(90, TimeUnit.SECONDS).subscribe(
            result -> {
                assertTrue(false);
            }, error -> {
                assertTrue(error instanceof FetchRecipeException);
            });
    }

    @Test
    public void fetchFailsWhenThereIsNoProjectFile() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitHubRecipeSource.of(fs);

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-no-project"))
            .result();

        task.timeout(90, TimeUnit.SECONDS).subscribe(
            result -> {
                assertTrue(false);
            }, error -> {
                assertTrue(error instanceof FetchRecipeException);
            });
    }

    @Test
    public void fetchGivesProgressEvents() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitHubRecipeSource.of(fs);

        final Observable<Event> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-c")).states();

        final List<Event> actual = task.timeout(90, TimeUnit.SECONDS).toList().blockingGet();

        assertTrue(actual.size() > 1);
    }

    @Test
    public void fetchRecognizesTarget() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitHubRecipeSource.of(fs);

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-d"))
            .result();

        final Optional<String> expected = Optional.of("target-d");

        final Optional<String> actual = task.timeout(90, TimeUnit.SECONDS).blockingGet()
            .versions.get(SemanticVersion.of(0, 1)).target;

        assertEquals(expected, actual);
    }
}