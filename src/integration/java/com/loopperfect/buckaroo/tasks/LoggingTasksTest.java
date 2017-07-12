package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Event;
import io.reactivex.Observable;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class LoggingTasksTest {

    @Test
    public void log() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Observable<Event> task = LoggingTasks.log(fs, "integration test");

        final CountDownLatch latch = new CountDownLatch(1);

        task.subscribe(
            result -> {
                latch.countDown();
            }, error -> {
                error.printStackTrace();
            });

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }
}
