package com.loopperfect.buckaroo.tasks;

import com.google.common.jimfs.Jimfs;
import com.google.common.util.concurrent.SettableFuture;
import io.reactivex.Observable;
import org.junit.Test;

import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public final class DownloadTaskTest {

    @Test
    public void completes() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final CountDownLatch latch = new CountDownLatch(1);

        DownloadTask.download(
            new URL("http://www.google.com"),
            fs.getPath("test.txt").toAbsolutePath())
            .subscribe(
                next -> {

                },
                error -> {

                },
                () -> {
                    assertTrue(true);
                    latch.countDown();
                });

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void notifiesAboutProgress() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final SettableFuture<Integer> future = SettableFuture.create();

        int count = DownloadTask.download(
            new URL("http://www.google.com"),
            fs.getPath("test.txt").toAbsolutePath())
            .reduce(0, (x, p) -> x+1)
            .blockingGet();

        assertTrue ( count > 5 );
    }

    @Test
    public void callsbackAfterComplete() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final SettableFuture<Boolean> a = SettableFuture.create();
        final SettableFuture<Boolean> b = SettableFuture.create();

        final Path path = fs.getPath("test.txt").toAbsolutePath();

        final Observable<DownloadProgress> observable = DownloadTask.download(
            new URL("http://www.google.com"),
            path);

        observable.subscribe(
                next -> {

                },
                error -> {
                    a.set(false);
                },
                () -> {
                    a.set(true);
                });

        // Wait for the observable to complete...
        assertTrue(a.get());

        Files.delete(path);

        // ... then add another subscription
        observable.subscribe(
            next -> {

            },
            error -> {
                b.set(false);
            },
            () -> {
                b.set(true);
            });

        // It should also get called!
        assertTrue(b.get());
    }

    @Test
    public void createsParentDirectories() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Path target = fs.getPath("/a/b/c/test.txt");

        DownloadTask.download(new URL("http://www.google.com"), target).toList()
            .blockingGet();

        assertTrue(Files.exists(target));
    }

    @Test
    public void createsParentDirectories2() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Path target = fs.getPath("a/b/c/test.txt").toAbsolutePath();

        DownloadTask.download(new URL("http://www.google.com"), target).toList()
            .blockingGet();

        assertTrue(Files.exists(target));
    }
}
