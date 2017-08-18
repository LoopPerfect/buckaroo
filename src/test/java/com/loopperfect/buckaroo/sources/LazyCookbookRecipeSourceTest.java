package com.loopperfect.buckaroo.sources;

import com.google.common.jimfs.Jimfs;
import com.google.common.util.concurrent.SettableFuture;
import com.loopperfect.buckaroo.*;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public final class LazyCookbookRecipeSourceTest {

    @Test
    public void fetchFailsGracefully() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = LazyCookbookRecipeSource.of(
            fs.getPath(System.getProperty("user.home"), ".buckaroo", "buckaroo-recipes"));

        final CountDownLatch latch = new CountDownLatch(1);

        recipeSource.fetch(RecipeIdentifier.of(Identifier.of("nosuchorg"), Identifier.of("nosuchrecipe")))
            .result()
            .subscribe(x -> {

            }, error -> {
                latch.countDown();
            });

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void givesInformativeParseErrors() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        EvenMoreFiles.writeFile(
            fs.getPath(System.getProperty("user.home"), ".buckaroo", "buckaroo-recipes", "recipes", "org", "example.json"),
            "{ \"content\": \"this is an invalid recipe\" }");

        final RecipeSource recipeSource = LazyCookbookRecipeSource.of(
            fs.getPath(System.getProperty("user.home"), ".buckaroo", "buckaroo-recipes"));

        final SettableFuture<Throwable> futureError = SettableFuture.create();

        recipeSource.fetch(RecipeIdentifier.of(Identifier.of("org"), Identifier.of("example")))
            .result()
            .subscribe(x -> {

            }, error -> {
                futureError.set(error);
            });

        final Throwable exception = futureError.get(5000L, TimeUnit.MILLISECONDS);

        assertTrue(exception instanceof RecipeFetchException);
    }
}