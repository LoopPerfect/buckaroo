package com.loopperfect.buckaroo.tasks;

import com.google.common.hash.HashCode;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.EvenMoreFiles;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.RemoteFile;
import org.junit.Test;

import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public final class CacheTasksTest {

    public CacheTasksTest() {

    }

    @Test
    public void downloadToCachePopulatesTheCache() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RemoteFile remoteFile = RemoteFile.of(
            new URL("https://raw.githubusercontent.com/njlr/test-lib-a/3af013452fe6b448b1cb33bb81bb19da690ec764/BUCK"),
            HashCode.fromString("bb7220f89f404f244ff296b0fba7a70166de35a49094631c9e8d3eacda58d67a"));

        final Path cachePath = CacheTasks.getCachePath(fs, remoteFile);

        CacheTasks.downloadToCache(fs, remoteFile).toList().blockingGet();

        assertTrue(Files.exists(cachePath));
    }

    @Test
    public void downloadToCacheBustsBadCaches() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RemoteFile remoteFile = RemoteFile.of(
            new URL("https://raw.githubusercontent.com/njlr/test-lib-a/3af013452fe6b448b1cb33bb81bb19da690ec764/BUCK"),
            HashCode.fromString("bb7220f89f404f244ff296b0fba7a70166de35a49094631c9e8d3eacda58d67a"));

        final Path cachePath = CacheTasks.getCachePath(fs, remoteFile);

        EvenMoreFiles.writeFile(cachePath, "invalid");

        CacheTasks.downloadToCache(fs, remoteFile).toList().blockingGet();

        assertTrue(Files.exists(cachePath));
        assertEquals(remoteFile.sha256, EvenMoreFiles.hashFile(cachePath));
    }

    @Test
    public void cacheWorksTwiceInARow() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RemoteFile remoteFile = RemoteFile.of(
            new URL("https://raw.githubusercontent.com/njlr/test-lib-a/3af013452fe6b448b1cb33bb81bb19da690ec764/BUCK"),
            HashCode.fromString("bb7220f89f404f244ff296b0fba7a70166de35a49094631c9e8d3eacda58d67a"));

        final Path path = fs.getPath("test.txt");

        CacheTasks.downloadUsingCache(remoteFile, path).toList().blockingGet();

        final List<Event> events = CacheTasks.downloadUsingCache(remoteFile, path).toList().blockingGet();

        assertTrue(Files.exists(path));
        assertEquals(remoteFile.sha256, EvenMoreFiles.hashFile(path));
        assertTrue(events.stream().noneMatch(x -> x instanceof DownloadProgress));
    }
}
