package com.loopperfect.buckaroo.gitlab;

import com.google.common.jimfs.Jimfs;
import com.google.common.util.concurrent.SettableFuture;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.RecipeSource;
import com.loopperfect.buckaroo.sources.GitProviderRecipeSource;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class GitLabGitProviderTest {

    @Test
    public void test() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitProviderRecipeSource.of(
            fs,
            GitLabGitProvider.of());

        final SettableFuture<Boolean> future = SettableFuture.create();

        recipeSource.fetch(RecipeIdentifier.of("gitlab", "njlr", "hello-buckaroo"))
            .result()
            .subscribe(recipe -> {
                assertEquals("hello-buckaroo", recipe.name);
                assertTrue(!recipe.versions.isEmpty());
                future.set(true);
            }, error -> {
                error.printStackTrace();
                future.setException(error);
                future.set(false);
            });

        assertTrue(future.get(20000L, TimeUnit.MILLISECONDS));
    }
}
