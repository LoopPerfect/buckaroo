package com.loopperfect.buckaroo.serialization;

import com.google.gson.*;
import com.loopperfect.buckaroo.*;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Optional;

public final class RecipeVersionUnlockedDeserializer implements JsonDeserializer<RecipeVersionUnlocked> {

    @Override
    public RecipeVersionUnlocked deserialize(
        final JsonElement jsonElement, final Type type, final JsonDeserializationContext context)
        throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (!jsonObject.has("source")) {
            throw new JsonParseException("A recipe version must have a source. ");
        }

        final JsonElement sourceJsonElement = jsonObject.get("source");

        final Either<GitCommit, RemoteArchiveUnlocked> source = sourceJsonElement.isJsonPrimitive() ?
            Either.left(context.deserialize(sourceJsonElement, GitCommit.class)) :
            Either.right(context.deserialize(sourceJsonElement, RemoteArchiveUnlocked.class));

        final Optional<String> target = jsonObject.has("target") ?
            Optional.of(jsonObject.get("target").getAsString()) :
            Optional.empty();

        final Optional<DependencyGroup> dependencies = jsonObject.has("dependencies") ?
            Optional.of(context.deserialize(jsonObject.get("dependencies"), DependencyGroup.class)) :
            Optional.empty();

        final Optional<URI> buckResource = jsonObject.has("buck") ?
            Optional.of(
                jsonObject.get("buck").isJsonObject() ?
                    context.deserialize(jsonObject.getAsJsonObject("buck").get("url"), URI.class) :
                    context.deserialize(jsonObject.get("buck"), URI.class)) :
            Optional.empty();

        return RecipeVersionUnlocked.of(source, target, dependencies, buckResource);
    }
}
