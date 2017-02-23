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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public final class Routines {

    private Routines() {

    }

    public static final IO<String> buckarooDirectory =
            context -> Paths.get(context.fs().userHomeDirectory().toString(), ".buckaroo/").toString();

    public static final IO<String> configFilePath =
            buckarooDirectory.map(x -> Paths.get(x, "config.json").toString());

    public static final IO<String> projectFilePath =
            context -> Paths.get(context.fs().workingDirectory().toString(), "buckaroo.json").toString();

    public static IO<Either<IOException, Project>> readProject(final String path) {
        Preconditions.checkNotNull(path);
        return context -> context.fs().readFile(path).join(
                Either::left,
                content -> Serializers.parseProject(content).leftProjection(IOException::new));
    }

    public static IO<Optional<IOException>> writeProject(final String path, final Project project, final boolean overwrite) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(project);
        return IO.writeFile(path, Serializers.serialize(project), overwrite);
    }

    public static IO<Either<IOException, BuckarooConfig>> readConfig(final String path) {
        Preconditions.checkNotNull(path);
        return context -> context.fs().readFile(path).join(
                Either::left,
                content -> {
                    Preconditions.checkNotNull(content);
                    return Serializers.parseConfig(content).leftProjection(IOException::new);
                });
    }

    private static IO<Either<IOException, Recipe>> readRecipe(final String path) {
        Preconditions.checkNotNull(path);
        return context -> context.fs().readFile(path).join(
                Either::left,
                content -> Serializers.parseRecipe(content).leftProjection(IOException::new));
    }

    private static boolean isJsonFile(final String path) {
        return Files.getFileExtension(path).equalsIgnoreCase("json");
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

    private static IO<Either<IOException, CookBook>> readCookBook(final String path) {
        Preconditions.checkNotNull(path);
        return IO.of(x -> x.fs().listFiles(Paths.get(path, "/recipes/").toString()))
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
                        .flatMap(path -> context -> context.fs().getPath(path, remoteCookBook.name.toString()).toString())
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
        return IO.of(context -> context.fs().getPath(path).toFile())
                .flatMap(file -> IO.sequence(ImmutableList.of(
                        context -> context.git().clone(file, gitCommit.url),
                        context -> context.git().checkout(file, gitCommit.commit),
                        context -> context.git().pull(file)))
                        .then(context -> context.git().status(file)));
    }
}
