package com.loopperfect.buckaroo.sources;

import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.RecipeSource;
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
}