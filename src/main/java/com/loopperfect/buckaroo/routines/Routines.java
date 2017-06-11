package com.loopperfect.buckaroo.routines;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.google.common.hash.HashCode;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.crypto.Hash;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.serialization.Serializers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.loopperfect.buckaroo.Either.left;

public final class Routines {

    private Routines() {

    }

    public static IO<Optional<IOException>> upgrade(
        final String buckarooDirectory, final RemoteCookBook cookBook) {
        Preconditions.checkNotNull(buckarooDirectory);
        Preconditions.checkNotNull(cookBook);
        final String cookBookPath = buckarooDirectory + "/" + cookBook.name;
        return ensureCheckout(cookBookPath, GitCommit.of(cookBook.url, "master"));
    }

    public static IO<Optional<IOException>> upgradeForConfig(final Path configFilePath) {
        Preconditions.checkNotNull(configFilePath);
        return IO.value(configFilePath)
            .flatMap(x -> readConfig(x.toString()))
            .flatMap(readConfigResult -> readConfigResult.join(
                e -> IO.value(Optional.of(e)),
                config -> buckarooDirectory.flatMap(path -> continueUntilPresent(
                        config.cookBooks.stream()
                        .map(cookBook -> IO.println("Upgrading " + cookBook.name + "...")
                            .then(upgrade(path, cookBook)))
                        .collect(ImmutableList.toImmutableList())))));
    }

    public static IO<Optional<IOException>> ensureConfig =
        IO.of(context -> {
            Preconditions.checkNotNull(context);
            return context.fs().getPath(
                context.fs().homeDirectory(),
                "/",
                ".buckaroo",
                "/",
                "config.json");
        }).flatMap(configFile -> IO.of(context -> {
            Preconditions.checkNotNull(context);
            try {
                if (!context.fs().exists(configFile.toString())) {
                    final String defaultConfig = Resources.toString(
                        Resources.getResource("com.loopperfect.buckaroo/DefaultConfig.txt"),
                        Charsets.UTF_8);
                    context.fs().writeFile(configFile.toString(), defaultConfig);
                    return upgradeForConfig(configFile).run(context);
                }
                return Optional.empty();
            } catch (final IOException e) {
                return Optional.of(e);
            }
        }));

    public static final IO<String> buckarooDirectory =
        context -> Paths.get(context.fs().homeDirectory(), ".buckaroo/").toString();

    public static final IO<String> configFilePath =
        buckarooDirectory.map(x -> Paths.get(x, "config.json").toString());

    public static final IO<String> projectFilePath =
        context -> Paths.get(context.fs().workingDirectory(), "buckaroo.json").toString();

    public static IO<Either<IOException, Project>> readProject(final String path) {
        Preconditions.checkNotNull(path);
        return context -> context.fs().readFile(path).join(
                Either::<IOException, Project>left,
                content -> Serializers.parseProject(content).leftProjection(IOException::new));
    }

    public static IO<Optional<IOException>> writeProject(
            final String path, final Project project, final boolean overwrite) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(project);
        return IO.writeFile(path, Serializers.serialize(project), overwrite);
    }

    public static IO<Either<IOException, BuckarooConfig>> readConfig(final String path) {
        Preconditions.checkNotNull(path);
        return context -> context.fs().readFile(path).join(
                Either::<IOException, BuckarooConfig>left,
                content -> {
                    Preconditions.checkNotNull(content);
                    return Serializers.parseConfig(content).leftProjection(IOException::new);
                });
    }

    private static IO<Either<IOException, Recipe>> readRecipe(final String path) {
        Preconditions.checkNotNull(path);
        return IO.of(x -> x.fs().readFile(path))
                .map(x -> x.join(
                        Either::<IOException, Recipe>left,
                        content -> Serializers.parseRecipe(content).leftProjection(IOException::new)));
    }

    public static <L, R> IO<Either<L, ImmutableList<R>>> allOrNothing(final ImmutableList<IO<Either<L, R>>> xs) {
        Preconditions.checkNotNull(xs);
        return context -> {
            Preconditions.checkNotNull(context);
            final ImmutableList.Builder builder = ImmutableList.builder();
            for (final IO<Either<L, R>> x : xs) {
                final Either<L, R> result = x.run(context);
                if (result.left().isPresent()) {
                    return left(result.left().get());
                }
                builder.add(result.right().get());
            }
            return Either.right(builder.build());
        };
    }

    public static <T> IO<Optional<T>> continueUntilPresent(final ImmutableList<IO<Optional<T>>> xs) {
        Preconditions.checkNotNull(xs);
        return context -> {
            Preconditions.checkNotNull(context);
            for (final IO<Optional<T>> x : xs) {
                final Optional<T> result = x.run(context);
                if (result.isPresent()) {
                    return result;
                }
            }
            return Optional.empty();
        };
    }

    private static IO<Either<IOException, ImmutableList<Identifier>>> listRecipesForOrganization(final String path) {
        Preconditions.checkNotNull(path);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.fs().listFiles(path)
                    .rightProjection(files -> files.stream()
                            .filter(file -> context.fs().isFile(file) &&
                                    Files.getFileExtension(file).equalsIgnoreCase("json"))
                            .map(file -> context.fs().getPath(file).getFileName().toString())
                            .map(file -> file.substring(0, file.length() - ".json".length()))
                            .filter(Identifier::isValid)
                            .map(Identifier::of)
                            .distinct()
                            .collect(ImmutableList.toImmutableList()));
        };
    }

    private static IO<Either<IOException, ImmutableList<Identifier>>> listOrganizationsForCookBook(final String cookBookPath) {
        Preconditions.checkNotNull(cookBookPath);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.fs().listFiles(context.fs().getPath(cookBookPath, "recipes").toString())
                    .rightProjection(files -> files.stream()
                            .filter(file -> Files.getFileExtension(file).equalsIgnoreCase("json") &&
                                    context.fs().isFile(file))
                            .map(file -> context.fs().getPath(file).getFileName().toString())
                            .map(file -> file.substring(0, file.length() - ".json".length()))
                            .filter(Identifier::isValid)
                            .map(Identifier::of)
                            .distinct()
                            .collect(ImmutableList.toImmutableList()));
        };
    }

    public static IO<Either<IOException, Organization>> readOrganization(
        final String path, final Identifier identifier) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(identifier);
        return listRecipesForOrganization(path + "/" + identifier.name + "/")
            .flatMap(x -> x.join(
                error -> IO.value(Either.<IOException, Organization>left(error)),
                identifiers -> allOrNothing(
                    identifiers.stream()
                        .map(i -> readRecipe(path + "/" + identifier.name + "/" + i.name + ".json")
                            .map(y -> y.rightProjection(z -> Maps.immutableEntry(i, z)))
                            .map(y -> y.leftProjection(z ->
                                new IOException("Error reading recipe at " + path + "/" + identifier.name + "/" + i.name + ".json", z))))
                        .collect(ImmutableList.toImmutableList()))
                    .map(y -> y.rightProjection(
                        recipes -> Organization.of(identifier.name, recipes.stream()
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))))));
    }

    private static IO<Either<IOException, CookBook>> readCookBook(final String path) {
        Preconditions.checkNotNull(path);
        return listOrganizationsForCookBook(path)
            .flatMap(x -> x.join(
                error -> IO.value(Either.<IOException, CookBook>left(error)),
                identifiers -> allOrNothing(
                    identifiers.stream()
                        .map(identifier -> readOrganization(path + "/recipes", identifier)
                            .map(i -> i.rightProjection(j -> Maps.immutableEntry(identifier, j))))
                        .collect(ImmutableList.toImmutableList()))
                    .map(y -> y.rightProjection(organizations -> CookBook.of(organizations.stream()
                        .collect(ImmutableMap.toImmutableMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue)))))));
    }

    public static IO<Either<IOException, ImmutableList<CookBook>>> readCookBooks(final BuckarooConfig config) {
        Preconditions.checkNotNull(config);
        return allOrNothing(config.cookBooks.stream()
            .map(remoteCookBook -> buckarooDirectory
                .flatMap(path -> context -> context.fs()
                    .getPath(path, remoteCookBook.name.toString()).toString())
                .flatMap(Routines::readCookBook)
                .map(x -> x.leftProjection(y -> new IOException("Error reading " + remoteCookBook.name, y))))
            .collect(ImmutableList.toImmutableList()));
    }

    public static IO<Optional<IOException>> fetchSource(final String path, final Either<GitCommit, RemoteArchive> source) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(source);
        return Either.join(
            source,
            gitCommit -> ensureCheckout(path, gitCommit),
            remoteFile -> fetchAndUnzip(path, remoteFile));
    }


    /**
     * Fetches a remote-file and downloads it to the given path.
     * If a file is already present, then its hash is checked against what is expected.
     *
     * An error is returned if the process failed in any way, and a nothing otherwise.
     *
     * The process may fail in a number of ways:
     *
     *  - There is already a directory at the target path
     *  - There is already a file at the target path and it has the wrong hash
     *  - It was not possible to download the file
     *  - The file could not be written to the target path
     *  - The hash of the downloaded file did not match the expected hash
     *
     * @return  An error if the process failed in any way,
     *          and a nothing otherwise.
     */
    public static IO<Optional<IOException>> fetchRemoteFile(final String path, final RemoteFile remoteFile) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(remoteFile);
        return context -> {
            Preconditions.checkNotNull(context);
            if (context.fs().exists(path)) {
                if (context.fs().isDirectory(path)) {
                    return Optional.of(new IOException("There is a directory at " + path + "! "));
                }
            } else {
                final Optional<IOException> download =
                    context.http().download(remoteFile.url, context.fs().getPath(path));
                if (download.isPresent()) {
                    return download;
                }
            }
            final Either<IOException, HashCode> actual = Hash.sha256(context.fs().getPath(path));
            if (!Objects.equals(actual, Either.right(remoteFile.sha256))) {
                return Optional.of(new IOException("Hash mismatch! Expected " + remoteFile.sha256 +
                    " but got " + actual.join(l -> "<invalid>", HashCode::toString) + ". "));
            }
            return Optional.empty();
        };
    }

    public static IO<Optional<IOException>> fetchAndUnzip(final String path, final RemoteArchive remoteFile) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(remoteFile);
        return context -> {
            Preconditions.checkNotNull(context);
            final Optional<IOException> fetchResult = fetchRemoteFile(path + ".zip", remoteFile.asRemoteFile()).run(context);
            if (fetchResult.isPresent()) {
                return fetchResult;
            }
            final Path zipPath = context.fs().getPath(path + ".zip");
            final Path targetPath = context.fs().getPath(path);
            return com.loopperfect.buckaroo.io.Files.unzip(
                zipPath, targetPath, remoteFile.subPath.map(x -> context.fs().getPath(x)));
        };
    }

    public static IO<Optional<IOException>> ensureCheckout(final String path, final GitCommit gitCommit) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(gitCommit);
        return IO.of(context -> context.fs().getPath(path).toFile())
            // Try to clone and pull, ignoring any errors...
            .flatMap(file -> IO.sequence(ImmutableList.of(
                context -> context.git().clone(file, gitCommit.url),
                context -> context.git().pull(file)))
                // Now if we succeeded, the checkout should not fail...
                .then(context -> context.git().checkout(file, gitCommit.commit)
                    .map(e -> new IOException("Could not checkout " + gitCommit.encode() + " to " + path, e))));
    }

    /**
     * Loads the Buckaroo config from the expected path.
     * If there is no config available, the default config is created automatically.
     *
     * @return  An error if the process failed in any way,
     *          and the Buckaroo config otherwise.
     */
    public static IO<Either<IOException, BuckarooConfig>> loadConfig = ensureConfig
        .flatMap(x -> Optionals.join(x,
            i -> IO.value(left(i)),
            () -> configFilePath.flatMap(Routines::readConfig)));

    /**
     * Gets a UUID for this user on this system.
     * If the process fails, then a new UUID is generated.
     *
     * @return  A random UUID tied to this system
     */
    public static final IO<String> getIdentifier = IO.of(context -> {
        Preconditions.checkNotNull(context);
        final String path = context.fs().getPath(
            Routines.buckarooDirectory.run(context),
            "user-uuid.txt").toString();
        if (!context.fs().exists(path)) {
            final String identifier = UUID.randomUUID().toString();
            context.fs().writeFile(path, identifier);
            return identifier;
        }
        return context.fs().readFile(path).join(
            e -> UUID.randomUUID().toString(),
            x -> x.trim());
    });
}
