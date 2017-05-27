package com.loopperfect.buckaroo.serialization;

import com.google.gson.*;
import com.loopperfect.buckaroo.*;

import java.lang.reflect.Type;
import java.util.Optional;

public final class RecipeVersionDeserializer implements JsonDeserializer<RecipeVersion> {

    @Override
    public RecipeVersion deserialize(
            final JsonElement jsonElement, final Type type, final JsonDeserializationContext context)
            throws JsonParseException {

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

        final Optional<DependencyGroup> dependencies = jsonObject.has("dependencies") ?
                Optional.of(context.deserialize(jsonObject.get("dependencies"), DependencyGroup.class)) :
                Optional.empty();

        final Optional<RemoteFile> buckResource = jsonObject.has("buck") ?
                Optional.of(context.deserialize(jsonObject.get("buck"), RemoteFile.class)) :
                Optional.empty();

        return RecipeVersion.of(source, target, dependencies, buckResource);
    }
}
