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

        final JsonElement sourceJsonElement = jsonObject.get("source");

        final Either<GitCommit, RemoteArchive> source = sourceJsonElement.isJsonPrimitive() ?
            Either.left(context.deserialize(sourceJsonElement, GitCommit.class)) :
            Either.right(context.deserialize(sourceJsonElement, RemoteArchive.class));

        final Optional<String> target = jsonObject.has("target") ?
                Optional.of(jsonObject.get("target").getAsString()) :
                Optional.empty();

        final DependencyGroup dependencies = jsonObject.has("dependencies") ?
                context.deserialize(jsonObject.get("dependencies"), DependencyGroup.class) :
                DependencyGroup.of();

        final Optional<Resource> buckResource = jsonObject.has("buck") ?
                Optional.of(context.deserialize(jsonObject.get("buck"), Resource.class)) :
                Optional.empty();

        return RecipeVersion.of(source, target, dependencies, buckResource);
    }
}
