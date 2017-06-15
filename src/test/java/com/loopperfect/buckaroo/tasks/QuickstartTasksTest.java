package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.EvenMoreFiles;
import com.loopperfect.buckaroo.Event;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

public final class QuickstartTasksTest {

    @Test
    public void quickstartCreatesExpectedFiles() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final List<Event> events = QuickstartTasks.quickstartInWorkingDirectory(fs).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));
        assertTrue(Files.exists(fs.getPath(".buckconfig")));
        assertTrue(Files.exists(fs.getPath("BUCKAROO_DEPS")));
    }
}