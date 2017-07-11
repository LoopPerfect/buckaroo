package com.loopperfect.buckaroo.tasks;

import com.google.common.hash.HashCode;
import com.google.common.jimfs.Jimfs;
import com.google.common.util.concurrent.SettableFuture;
import com.loopperfect.buckaroo.RemoteFile;
import com.loopperfect.buckaroo.EvenMoreFiles;
import org.junit.Test;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CommonTasksIntegrationsTests {

    @Test
    public void downloadRemoteFileCompletes() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RemoteFile remoteFile = RemoteFile.of(
            new URL("https://raw.githubusercontent.com/nikhedonia/googletest/665327f0141d4a4fc4f2496e781dce436d742645/BUCK"),
            HashCode.fromString("d976069f5b47fd8fc57201f47026a70dee4e47b3141ac23567b8a0c56bf9288c"));

        final SettableFuture<Boolean> future = SettableFuture.create();

        CommonTasks.downloadRemoteFile(fs, remoteFile, fs.getPath("test.txt").toAbsolutePath())
            .subscribe(
                next -> {

                },
                error -> {
                    future.set(false);
                },
                () -> {
                    future.set(true);
                });

        assertTrue(future.get(30, TimeUnit.SECONDS));
    }

    @Test
    public void downloadRemoteFileChecksHash() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Path target = fs.getPath("test.txt").toAbsolutePath();

        EvenMoreFiles.writeFile(target, "hello", Charset.defaultCharset(), false);

        final RemoteFile remoteFile = RemoteFile.of(
            new URL("https://raw.githubusercontent.com/nikhedonia/googletest/665327f0141d4a4fc4f2496e781dce436d742645/BUCK"),
            HashCode.fromString("d976069f5b47fd8fc57201f47026a70dee4e47b3141ac23567b8a0c56bf9288c"));

        final SettableFuture<Boolean> future = SettableFuture.create();

        CommonTasks.downloadRemoteFile(fs, remoteFile, target)
            .subscribe(
                next -> {

                },
                error -> {
                    // true here because it should fail
                    future.set(true);
                },
                () -> {
                    future.set(false);
                });

        assertTrue(future.get(30, TimeUnit.SECONDS));
    }

    @Test
    public void writeAndReadFile() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Path target = fs.getPath("test.txt").toAbsolutePath();
        final String content = "This is a test\n Testing... testing... 123";

        CommonTasks.writeFile(content, target, false).blockingGet();

        final String readContent = CommonTasks.readFile(target).blockingGet();

        assertEquals(content, readContent);
    }
}