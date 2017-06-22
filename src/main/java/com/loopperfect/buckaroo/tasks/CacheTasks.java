package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.events.FileCopyEvent;
import com.loopperfect.buckaroo.events.FileHashEvent;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.net.URL;
import java.nio.file.*;
import java.util.Optional;

public final class CacheTasks {

    private CacheTasks() {

    }

    public static Path getCacheFolder(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        // macOS
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            return fs.getPath(System.getProperty("user.home"), "Library", "Caches", "Buckaroo");
        }

        return fs.getPath(System.getProperty("user.home"), ".buckaroo", "caches");
    }

    public static Path getCachePath(final FileSystem fs, final RemoteFile file) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(file);

        return fs.getPath(
            getCacheFolder(fs).toString(),
            file.sha256.toString() + URLUtils.getExtension(file.url).map(x -> "." + x).orElse(""));
    }

    public static Path getCachePath(final FileSystem fs, final URL url, final Optional<String> extension) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(extension);

        return fs.getPath(
            getCacheFolder(fs).toString(),
            StringUtils.escapeStringGitHubStyle(url.toString()) +
            extension.map(x -> "." + x).orElseGet(
                () -> URLUtils.getExtension(url).map(x -> "." + x).orElse("")));
    }

    public static Path getCachePath(final FileSystem fs, final URL url) {
        return getCachePath(fs, url, Optional.empty());
    }

    public static Observable<Event> downloadToCache(final FileSystem fs, final RemoteFile file) {

        Preconditions.checkNotNull(file);

        final Path target = getCachePath(fs, file);

        // Does the file exist?
        return Observable.fromCallable(() -> Files.exists(target)).flatMap(fileExists -> {

            // Yes
            if (fileExists) {

                // Verify the hash
                final HashCode actual = EvenMoreFiles.hashFile(target);

                return MoreObservables.chain(

                    Observable.just(FileHashEvent.of(target, actual)),

                    (FileHashEvent fileHashEvent) -> {
                        // Does it match?
                        if (fileHashEvent.sha256.equals(file.sha256)) {

                            // Yes, so do nothing
                            return Observable.empty();
                        }

                        // No, so retry the download
                        return Observable.concat(

                            // Delete the file
                            CommonTasks.deleteIfExists(target)
                                .toObservable()
                                .cast(Event.class),

                            // Retry the download
                            CommonTasks.downloadRemoteFile(fs, file, target)
                        );
                    });
            }

            // No...
            // ... so download the file!
            return CommonTasks.downloadRemoteFile(fs, file, target);
        });
    }

    public static Observable<Event> downloadToCache(final FileSystem fs, final URL url) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(url);

        final Path cachePath = getCachePath(fs, url);

        if (Files.exists(cachePath)) {
            return Observable.empty();
        }

        return DownloadTask.download(url, cachePath).cast(Event.class);
    }

    public static Observable<Event> downloadUsingCache(final RemoteFile file, final Path target) {

        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(target);

        final FileSystem fs = target.getFileSystem();
        final Path cachePath = getCachePath(fs, file);

        return Observable.concat(
            downloadToCache(fs, file),
            CommonTasks.copy(cachePath, target, StandardCopyOption.REPLACE_EXISTING)
                .toObservable()
        );
    }

    public static Observable<Event> downloadUsingCache(final RemoteArchive archive, final Path target, final CopyOption... copyOptions) {

        Preconditions.checkNotNull(archive);
        Preconditions.checkNotNull(target);

        final FileSystem fs = target.getFileSystem();
        final Path cachePath = getCachePath(fs, archive.asRemoteFile());
        final Optional<Path> subPath = archive.subPath.map(x -> fs.getPath(fs.getSeparator(), x));

        return Observable.concat(
            downloadToCache(fs, archive.asRemoteFile()),
            CommonTasks.unzip(cachePath, target, subPath, copyOptions).toObservable()
        );
    }

    public static Single<FileCopyEvent> ensureCloneAndCheckout(final FileSystem fs, final GitCommit commit, final Path target) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(commit);

        final Path cachePath = fs.getPath(
            getCacheFolder(fs).toString(),
            StringUtils.escapeStringAsFilename(commit.url));

        return GitTasks
            .ensureCloneAndCheckout(commit, cachePath)
            .andThen(CommonTasks.copy(cachePath, target));
    }
}
