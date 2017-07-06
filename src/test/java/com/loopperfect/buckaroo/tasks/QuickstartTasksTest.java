package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;

import static org.junit.Assert.*;

public final class QuickstartTasksTest {

    @Test
    public void quickstartCreatesExpectedFiles() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        QuickstartTasks.quickstartInWorkingDirectory(fs).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));
        assertTrue(Files.exists(fs.getPath(".buckconfig")));
        assertTrue(Files.exists(fs.getPath("BUCKAROO_DEPS")));
    }
}