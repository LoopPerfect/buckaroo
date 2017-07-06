package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Event;
import io.reactivex.Observable;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public final class InitTasksTest {

    @Test
    public void initCreatesExpectedFiles() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        InitTasks.initWorkingDirectory(fs).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));
        assertTrue(Files.exists(fs.getPath(".buckconfig")));
    }

    @Test
    public void initFailsGracefullyWhenRunTwice() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        InitTasks.initWorkingDirectory(fs).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));
        assertTrue(Files.exists(fs.getPath(".buckconfig")));

        final CountDownLatch latch = new CountDownLatch(1);

        final Observable<Event> task = InitTasks.initWorkingDirectory(fs);

        task.subscribe(
            next -> {},
            error -> {
                latch.countDown();
            },
            () -> {

            });

        latch.await(5000L, TimeUnit.MILLISECONDS);

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));
        assertTrue(Files.exists(fs.getPath(".buckconfig")));
    }
}