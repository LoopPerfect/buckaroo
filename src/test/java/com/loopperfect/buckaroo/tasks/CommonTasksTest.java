package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class CommonTasksTest {

    @Test
    public void readProjectFileFailsGracefully() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final CountDownLatch latch = new CountDownLatch(1);

        CommonTasks.readProjectFile(fs.getPath("buckaroo.json")).result().subscribe(
            x -> {

            },
            error -> {
                latch.countDown();
            });

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void readRecipeFileFailsGracefully() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final CountDownLatch latch = new CountDownLatch(1);

        CommonTasks.readRecipeFile(fs.getPath("recipe.json")).subscribe(
            x -> {

            },
            error -> {
                latch.countDown();
            });

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }
}