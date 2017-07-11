package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.events.FileHashEvent;
import io.reactivex.Completable;
import io.reactivex.Observable;

import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheTasks {

    private static final Map<Path, Observable<Event>> inProgress = new ConcurrentHashMap<>();

    private CacheTasks() {

    }

    public static Path getCacheFolder(final String osName, final FileSystem fs) {

        Preconditions.checkNotNull(osName);
        Preconditions.checkNotNull(fs);

        // macOS
        if (osName.toLowerCase().contains("mac")) {
            return fs.getPath(System.getProperty("user.home"), "Library", "Caches", "Buckaroo");
        }

        // Linux
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return fs.getPath(System.getProperty("user.home"), ".cache", "Buckaroo");
        }

        return fs.getPath(System.getProperty("user.home"), ".buckaroo", "caches");
    }

    public static Path getCacheFolder(final FileSystem fs) {
        return getCacheFolder(System.getProperty("os.name"), fs);
    }

    public static Path getCachePath(final FileSystem fs, final RemoteFile file) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(file);

        return fs.getPath(
            getCacheFolder(fs).toString(),
            file.sha256.toString() +
                URLUtils.getExtension(file.url).map(x -> "." + x).orElse(""));
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

    private static Observable<Event> downloadToCacheUnsecure(final FileSystem fs, final RemoteFile file) {

        Preconditions.checkNotNull(file);

        final Path target = getCachePath(fs, file);

        // Does the file exist?
        return Observable.fromCallable(() -> Files.exists(target)).flatMap(fileExists -> {

            // Yes
            if (fileExists) {

                return CommonTasks.hash(target).flatMapObservable(

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

    public static Observable<Event> downloadToCache(final FileSystem fs, final RemoteFile file) {
        final Path cachePath = getCachePath(fs, file);
            synchronized (inProgress) {

                if (inProgress.containsKey(cachePath)) {
                    return inProgress.get(cachePath);
                }

                final Observable<Event> downloading = downloadToCacheUnsecure(fs, file)
                    .cast(Event.class)
                    .publish().autoConnect();

                inProgress.put(cachePath, downloading
                    .lastElement()
                    .toObservable()
                    .startWith(Notification.of("already downloading by another dependency")));

                return downloading;
            }
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

    public static Observable<Event> cloneAndCheckoutUsingCache(
        final GitCommit gitCommit, final Path targetDirectory, final StandardCopyOption... copyOptions) {

        Preconditions.checkNotNull(gitCommit);
        Preconditions.checkNotNull(targetDirectory);

        final Path cachePath = getCacheFolder(targetDirectory.getFileSystem())
            .resolve(StringUtils.escapeStringAsFilename(gitCommit.url));

        final Observable<Event> copy = Completable.fromAction(() ->
            EvenMoreFiles.copyDirectory(cachePath, targetDirectory, copyOptions))
            .toObservable();

        return Observable.concat(
            GitTasks.ensureCloneAndCheckout(gitCommit, cachePath, true)
            , copy);
    }
}
