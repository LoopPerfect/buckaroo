package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import com.google.gson.JsonSyntaxException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.serialization.Serializers;
import org.eclipse.jgit.api.Status;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public final class Routines {

    private Routines() {

    }

    private static final IO<Path> buckarooDirectory =
            context -> Paths.get(context.userHomeDirectory().toString(), ".buckaroo/");

    public static final IO<Path> configFilePath =
            buckarooDirectory.map(x -> Paths.get(x.toString(), "config.json"));

    public static final IO<Path> projectFilePath =
            context -> Paths.get(context.workingDirectory().toString(), "buckaroo.json");

    public static IO<Either<IOException, Project>> readProject(final Path path) {
        Preconditions.checkNotNull(path);
        return context -> context.readFile(path).join(
                Either::left,
                content -> Try.safe(
                        () -> Serializers.gson().fromJson(content, Project.class), JsonSyntaxException.class)
                        .leftProjection(IOException::new));
    }

    public static IO<Either<IOException, BuckarooConfig>> readConfig(final Path path) {
        Preconditions.checkNotNull(path);
        return context -> context.readFile(path).join(
                Either::left,
                content -> Try.safe(
                        () -> Serializers.gson().fromJson(content, BuckarooConfig.class), JsonSyntaxException.class)
                        .leftProjection(IOException::new));
    }

    private static IO<Either<IOException, Recipe>> readRecipe(final Path path) {
        Preconditions.checkNotNull(path);
        return context -> context.readFile(path).join(
                Either::left,
                content -> Try.safe(
                        () -> Serializers.gson().fromJson(content, Recipe.class), JsonSyntaxException.class)
                        .leftProjection(IOException::new));
    }

    private static boolean isJsonFile(final Path path) {
        return Files.getFileExtension(path.toString()).equalsIgnoreCase("json");
    }

    private static <T> ImmutableList<T> append(final ImmutableList<T> xs, final T x) {
        Preconditions.checkNotNull(xs);
        Preconditions.checkNotNull(x);
        return Streams.concat(xs.stream(), Stream.of(x))
                .collect(ImmutableList.toImmutableList());
    }

    public static <L, R> IO<Either<L, ImmutableList<R>>> allOrNothing(final ImmutableList<IO<Either<L, R>>> xs) {
        Preconditions.checkNotNull(xs);
        return context -> {
            Preconditions.checkNotNull(context);
            final ImmutableList.Builder builder = ImmutableList.builder();
            for (final IO<Either<L, R>> x : xs) {
                final Either<L, R> result = x.run(context);
                if (result.left().isPresent()) {
                    return Either.left(result.left().get());
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

    private static IO<Either<IOException, CookBook>> readCookBook(final Path path) {
        Preconditions.checkNotNull(path);
        return IO.of(x -> x.listFiles(Paths.get(path.toString(), "/recipes/")))
                .flatMap(listFiles -> listFiles.join(
                        error -> IO.value(Either.left(error)),
                        paths -> allOrNothing(paths.stream()
                                .filter(Routines::isJsonFile)
                                .map(Routines::readRecipe)
                                .collect(ImmutableList.toImmutableList()))
                                .map(x -> x.rightProjection(recipes -> CookBook.of(ImmutableSet.copyOf(recipes))))));
    }

    private static Path append(final Path a, final String... b) {
        return Paths.get(a.toString(), b);
    }

    public static IO<Either<IOException, ImmutableList<CookBook>>> readCookBooks(final BuckarooConfig config) {
        Preconditions.checkNotNull(config);
        return allOrNothing(config.cookBooks.stream()
                .map(remoteCookBook -> buckarooDirectory
                        .map(path -> append(path, remoteCookBook.name.toString()))
                        .flatMap(Routines::readCookBook))
                .collect(ImmutableList.toImmutableList()));
    }

    private static IO<Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>>> resolveDependencies(
            final Project project, final ImmutableList<CookBook> cookBooks) {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(cookBooks);
        final DependencyFetcher fetcher = CookbookDependencyFetcher.of(cookBooks);
        return IO.of(context -> DependencyResolver.resolve(project.dependencies, fetcher));
    }

    public static IO<Either<Exception, Status>> ensureCheckout(final String path, final GitCommit gitCommit) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(gitCommit);
        return IO.of(context -> context.getFS().getPath(path).toFile())
                .flatMap(file -> IO.sequence(ImmutableList.of(
                        context -> context.gitClone(file, gitCommit.url),
                        context -> context.gitCheckout(file, gitCommit.commit),
                        context -> context.gitPull(file)))
                        .then(context -> context.gitStatus(file)));
    }
}
