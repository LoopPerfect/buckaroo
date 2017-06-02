package com.loopperfect.buckaroo.github.serialization;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.github.GitHubRelease;
import com.loopperfect.buckaroo.serialization.*;

import java.io.StringReader;
import java.net.URL;

public final class GitHubSerializer {

    private GitHubSerializer() {

    }

    private static <T> Either<JsonParseException, T> parse(final String x, final Class<T> clazz) {
        Preconditions.checkNotNull(x);
        Preconditions.checkNotNull(clazz);
        try {
            final T t = gson.fromJson(
                new EmptyStringFailFastJsonReader(new StringReader(x)), clazz);
            return Either.right(t);
        } catch (final JsonParseException e) {
            return Either.left(e);
        } catch (final Throwable e) {
            return Either.left(new JsonParseException(e));
        }
    }

    private static <T> Either<JsonParseException, T> parse(final JsonElement x, final Class<T> clazz) {
        Preconditions.checkNotNull(x);
        Preconditions.checkNotNull(clazz);
        try {
            final T t = gson.fromJson(x, clazz);
            return Either.right(t);
        } catch (final JsonParseException e) {
            return Either.left(e);
        } catch (final Throwable e) {
            return Either.left(new JsonParseException(e));
        }
    }

    public static Either<JsonParseException, GitHubRelease> parseGitHubRelease(final String x) {
        Preconditions.checkNotNull(x);
        return parse(x, GitHubRelease.class);
    }

    public static Either<JsonParseException, GitHubRelease> parseGitHubRelease(final JsonElement x) {
        Preconditions.checkNotNull(x);
        return parse(x, GitHubRelease.class);
    }

    public static String serialize(final GitHubRelease gitHubRelease) {
        Preconditions.checkNotNull(gitHubRelease);
        return gson.toJson(gitHubRelease);
    }

    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(SemanticVersion.class, new SemanticVersionSerializer())
        .registerTypeAdapter(SemanticVersion.class, new SemanticVersionDeserializer())
        .registerTypeAdapter(GitCommit.class, new GitCommitSerializer())
        .registerTypeAdapter(GitCommit.class, new GitCommitDeserializer())
        .registerTypeAdapter(HashCode.class, new HashCodeDeserializer())
        .registerTypeAdapter(Identifier.class, new IdentifierSerializer())
        .registerTypeAdapter(Identifier.class, new IdentifierDeserializer())
        .registerTypeAdapter(RecipeIdentifier.class, new RecipeIdentifierSerializer())
        .registerTypeAdapter(RecipeIdentifier.class, new RecipeIdentifierDeserializer())
        .registerTypeAdapter(URL.class, new UrlDeserializer())
        .registerTypeAdapterFactory(new ImmutableListTypeAdapterFactory())
        .registerTypeAdapterFactory(new ImmutableMapTypeAdapterFactory())
        .registerTypeAdapter(GitHubRelease.class, new GitHubReleaseSerializer())
        .registerTypeAdapter(GitHubRelease.class, new GitHubReleaseDeserializer())
        .setPrettyPrinting()
        .create();
}
