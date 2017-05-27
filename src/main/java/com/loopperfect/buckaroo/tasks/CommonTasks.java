package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.EvenMoreFiles;
import com.loopperfect.buckaroo.serialization.Serializers;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public final class CommonTasks {

    private CommonTasks() {
        super();
    }

    private static String readEntireFile(final Path path) throws IOException {
        return Files.readAllLines(path, Charset.defaultCharset())
            .stream()
            .collect(Collectors.joining("\n"));
    }

    public static Single<Project> readProjectFile(final Path path) {
        Preconditions.checkNotNull(path);
        return Single.fromCallable(() ->
            Either.orThrow(Serializers.parseProject(readEntireFile(path))));
    }

    public static Single<DependencyLocks> readLockFile(final Path path) {
        Preconditions.checkNotNull(path);
        return Single.fromCallable(() ->
            Either.orThrow(Serializers.parseDependencyLocks(readEntireFile(path))));
    }

    public static Single<Recipe> readRecipeFile(final Path path) {
        Preconditions.checkNotNull(path);
        return Single.fromCallable(() ->
            Either.orThrow(Serializers.parseRecipe(readEntireFile(path))));
    }

    public static Single<BuckarooConfig> readConfigFile(final Path path) {
        Preconditions.checkNotNull(path);
        return Single.fromCallable(() ->
            Either.orThrow(Serializers.parseConfig(readEntireFile(path))));
    }

    public static Single<BuckarooConfig> readAndMaybeGenerateConfigFile(final FileSystem fs) {
        Preconditions.checkNotNull(fs);
        return Single.fromCallable(() -> {
            final Path configFilePath = fs.getPath(
                System.getProperty("user.home"),
                ".buckaroo",
                "buckaroo.json");
            if (!Files.exists(configFilePath)) {
                final String defaulConfigString = Resources.toString(
                    Resources.getResource("com.loopperfect.buckaroo/DefaultConfig.txt"),
                    Charset.defaultCharset());
                EvenMoreFiles.writeFile(configFilePath, defaulConfigString);
                return Either.orThrow(Serializers.parseConfig(defaulConfigString));
            }
            return Either.orThrow(Serializers.parseConfig(readEntireFile(configFilePath)));
        });
    }

    public static HashCode hashFile(final Path path) throws IOException {

        Preconditions.checkNotNull(path);

        final HashFunction hashFunction = Hashing.sha256();
        final HashCode hashCode = hashFunction.newHasher()
            .putBytes(MoreFiles.asByteSource(path).read())
            .hash();

        return hashCode;
    }

    public static Observable<DownloadProgress> downloadRemoteFile(final FileSystem fs, final RemoteFile remoteFile, final Path target) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(remoteFile);
        Preconditions.checkNotNull(target);

        return Observable.concat(
            // Does the file exist?
            Observable.fromCallable(() -> Files.exists(target))
                .flatMap(exists -> exists ?
                    // Then skip the download
                    Observable.empty() :
                    // Otherwise, download the file
                    DownloadTask.download(remoteFile.url, target)),
            // Verify the hash
            MoreCompletables.fromRunnable(() -> {
                final HashCode hashCode = CommonTasks.hashFile(target);
                if (!hashCode.equals(remoteFile.sha256)) {
                    throw new HashMismatchException(remoteFile.sha256, hashCode);
                }
            }).toObservable());
    }

    public static Observable<DownloadProgress> downloadRemoteArchive(final FileSystem fs, final RemoteArchive remoteArchive, final Path targetDirectory) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(remoteArchive);
        Preconditions.checkNotNull(targetDirectory);

        final Path zipFilePath = Paths.get(targetDirectory + ".zip");

        return Observable.concat(
            // Download the file
            CommonTasks.downloadRemoteFile(fs, remoteArchive.asRemoteFile(), zipFilePath),
            // Unpack the zip
            MoreCompletables.fromRunnable(() -> {
                EvenMoreFiles.unzip(
                    zipFilePath,
                    targetDirectory,
                    remoteArchive.subPath.map(subPath -> fs.getPath(subPath)));
            }).toObservable());
    }
}
