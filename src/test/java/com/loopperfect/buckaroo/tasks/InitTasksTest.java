package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class InitTasksTest {

    @Test
    public void initCreatesExpectedFiles() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();
        final Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());
        final Context ctx = Context.of(fs, scheduler);

        InitTasks.initWorkingDirectory(ctx).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));
        assertTrue(Files.exists(fs.getPath(".buckconfig")));
    }

    @Test
    public void readsProjectFileOnlyOnce() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();
        final Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());
        final Context ctx = Context.of(fs, scheduler);

        final List<Event> events = InitTasks.initWorkingDirectory(ctx).toList().blockingGet();

        // At some point we should read the project file...
        // ... but we should only do it once!
        assertEquals(1, events.stream().filter(x -> x instanceof ReadProjectFileEvent).count());
    }
}