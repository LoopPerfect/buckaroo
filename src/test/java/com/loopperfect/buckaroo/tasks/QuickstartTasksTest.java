package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.EvenMoreFiles;
import com.loopperfect.buckaroo.Event;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public final class QuickstartTasksTest {

    @Test
    public void quickstartCreatesExpectedFiles() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();
        final Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());
        final Context ctx = Context.of(fs, scheduler);

        final List<Event> events = QuickstartTasks.quickstartInWorkingDirectory(ctx).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));
        assertTrue(Files.exists(fs.getPath(".buckconfig")));
        assertTrue(Files.exists(fs.getPath("BUCKAROO_DEPS")));
    }
}