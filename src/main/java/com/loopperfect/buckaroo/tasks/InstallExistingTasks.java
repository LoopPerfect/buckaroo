package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.EvenMoreFiles;
import com.loopperfect.buckaroo.events.ReadLockFileEvent;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.javatuples.Pair;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class InstallExistingTasks {

    private InstallExistingTasks() {

    }

    private static Observable<Event> downloadResolvedDependency(final FileSystem fs, final ResolvedDependency resolvedDependency, final Path target) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(resolvedDependency);
        Preconditions.checkNotNull(target);

        final Observable<Event> downloadSourceCode = resolvedDependency.source.join(
            gitCommit -> Observable.error(new IOException("Git commit not supported yet. ")),
            remoteArchive -> CommonTasks.downloadRemoteArchive(fs, remoteArchive, target));

        final Path buckFilePath = fs.getPath(target.toString(), "BUCK");
        final Observable<Event> downloadBuckFile = Files.exists(buckFilePath) ?
            Observable.empty() :
            resolvedDependency.buckResource
                .map(x -> CommonTasks.downloadRemoteFile(fs, x, buckFilePath))
                .orElse(Observable.empty());

        final Path buckarooDepsFilePath = fs.getPath(target.toString(), "BUCKAROO_DEPS");
        final Observable<DownloadProgress> writeBuckarooDeps = MoreObservables.fromAction(() -> {
            EvenMoreFiles.writeFile(
                buckarooDepsFilePath,
                CommonTasks.generateBuckarooDeps(resolvedDependency.dependencies),
                Charset.defaultCharset(),
                true);
        });

        return Observable.concat(
            downloadSourceCode,
            downloadBuckFile,
            writeBuckarooDeps.cast(Event.class));
    }

    private static Observable<Event> installDependencyLock(final FileSystem fs, final DependencyLock lock) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(lock);

        final Path dependencyFolder = fs.getPath(
            "buckaroo", CommonTasks.toFolderName(lock.identifier)).toAbsolutePath();

        return downloadResolvedDependency(fs, lock.origin, dependencyFolder);
    }

    public static Observable<Event> installExistingDependenciesInWorkingDirectory(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        final Path lockFilePath = fs.getPath("buckaroo.lock.json").toAbsolutePath();

        return Observable.concat(

            // Do we have a lock file?
            Single.fromCallable(() -> Files.exists(lockFilePath)).flatMapObservable(hasBuckarooLockFile -> {
                if (hasBuckarooLockFile) {
                    // No need to generate one
                    return Observable.empty();
                }
                // Generate a lock file
                return ResolveTasks.resolveDependenciesInWorkingDirectory(fs);
            }),

            MoreSingles.chainObservable(

                // Read the lock file
                CommonTasks.readLockFile(fs.getPath("buckaroo.lock.json").toAbsolutePath())
                    .map(ReadLockFileEvent::of),

                (ReadLockFileEvent event) -> {

                    final ImmutableMap<DependencyLock, Observable<Event>> installs = event.locks.entries()
                        .stream()
                        .collect(ImmutableMap.toImmutableMap(i -> i, i -> installDependencyLock(fs, i)));

                    return MoreObservables.zipMaps(installs)
                        .map(x -> DependencyInstallationProgress.of(ImmutableMap.copyOf(x)));
                }
            ));
    }
}
