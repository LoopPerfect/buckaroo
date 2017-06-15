package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

public final class InitTasksTest {

    @Test
    public void initCreatesExpectedFiles() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        InitTasks.initWorkingDirectory(fs).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));
        assertTrue(Files.exists(fs.getPath(".buckconfig")));
    }
}