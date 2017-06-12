package com.loopperfect.buckaroo.github;

import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.FetchRecipeException;
import com.loopperfect.buckaroo.Recipe;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.RecipeSource;
import io.reactivex.Single;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public final class GitHubRecipeSourceTest {

    @Test
    public void fetch() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitHubRecipeSource.of(fs);

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-c"));

        task.timeout(90, TimeUnit.SECONDS).blockingGet();
    }

    @Test
    public void fetchFailsWhenThereAreNoReleases() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitHubRecipeSource.of(fs);

        final Single<Recipe> task = recipeSource.fetch(
            RecipeIdentifier.of("github", "njlr", "test-lib-no-releases"));

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
            RecipeIdentifier.of("github", "njlr", "test-lib-no-project"));

        task.timeout(90, TimeUnit.SECONDS).subscribe(
            result -> {
                assertTrue(false);
            }, error -> {
                assertTrue(error instanceof FetchRecipeException);
            });
    }
}