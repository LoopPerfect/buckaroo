package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.serialization.Serializers;
import org.eclipse.jgit.api.Status;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

public final class Routines {

    private Routines() {

    }

    public static final IO<String> buckarooDirectory =
            context -> Paths.get(context.fs().userHomeDirectory(), ".buckaroo/").toString();

    public static final IO<String> configFilePath =
        buckarooDirectory.map(x -> Paths.get(x, "config.json").toString());

    public static final IO<String> projectFilePath =
            context -> Paths.get(context.fs().workingDirectory(), "buckaroo.json").toString();

    public static IO<Either<IOException, Project>> readProject(final String path) {
        Preconditions.checkNotNull(path);
        return context -> context.fs().readFile(path).join(
                Either::left,
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
                Either::left,
                content -> {
                    Preconditions.checkNotNull(content);
                    return Serializers.parseConfig(content).leftProjection(IOException::new);
                });
    }

    private static IO<Either<IOException, Recipe>> readRecipe(final String path) {
        Preconditions.checkNotNull(path);
        return IO.of(x -> x.fs().readFile(path))
                .map(x -> x.join(
                        Either::left,
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
                error -> IO.value(Either.left(error)),
                identifiers -> allOrNothing(
                    identifiers.stream()
                        .map(i -> readRecipe(path + "/" + identifier.name + "/" + i.name + ".json")
                            .map(y -> y.rightProjection(z -> Maps.immutableEntry(i, z))))
                        .collect(ImmutableList.toImmutableList()))
                    .map(y -> y.rightProjection(
                        recipes -> Organization.of("FIXME", recipes.stream()
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))))));
    }

    private static IO<Either<IOException, CookBook>> readCookBook(final String path) {
        Preconditions.checkNotNull(path);
        return listOrganizationsForCookBook(path)
            .flatMap(x -> x.join(
                error -> IO.value(Either.left(error)),
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
                        .flatMap(Routines::readCookBook))
                .collect(ImmutableList.toImmutableList()));
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
