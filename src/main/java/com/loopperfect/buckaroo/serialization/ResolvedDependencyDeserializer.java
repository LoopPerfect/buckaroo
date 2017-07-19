package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.loopperfect.buckaroo.*;
import org.javatuples.Pair;

import java.lang.reflect.Type;
import java.util.Optional;

public final class ResolvedDependencyDeserializer implements JsonDeserializer<ResolvedDependency> {

    @Override
    public ResolvedDependency deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (!jsonObject.has("source")) {
            throw new JsonParseException("A recipe version must have a source. ");
        }
        final JsonElement sourceJsonElement = jsonObject.get("source");

        final Either<GitCommit, RemoteArchive> source = sourceJsonElement.isJsonPrimitive() ?
            Either.left(context.deserialize(sourceJsonElement, GitCommit.class)) :
            Either.right(context.deserialize(sourceJsonElement, RemoteArchive.class));

        final Optional<String> target = jsonObject.has("target") ?
            Optional.of(jsonObject.get("target").getAsString()) :
            Optional.empty();

        final Optional<RemoteFile> buckResource = jsonObject.has("buck") ?
            Optional.of(context.deserialize(jsonObject.get("buck"), RemoteFile.class)) :
            Optional.empty();

        final ImmutableList<RecipeIdentifier> dependencies = jsonObject.has("dependencies") ?
            context.deserialize(
                jsonObject.get("dependencies"),
                new TypeToken<ImmutableList<RecipeIdentifier>>() {}.getType()) :
            ImmutableList.of();

        final ImmutableList<ResolvedPlatformDependencies> platformDependencies = jsonObject.has("platformDependencies") ?
            context.deserialize(
                jsonObject.get("platformDependencies"),
                new TypeToken<ImmutableList<ResolvedPlatformDependencies>>() {}.getType()) :
            ImmutableList.of();

        return ResolvedDependency.of(source, target, buckResource, dependencies, platformDependencies);
    }
}
