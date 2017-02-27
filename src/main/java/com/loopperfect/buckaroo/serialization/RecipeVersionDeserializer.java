package com.loopperfect.buckaroo.serialization;

import com.google.gson.*;
import com.loopperfect.buckaroo.DependencyGroup;
import com.loopperfect.buckaroo.GitCommit;
import com.loopperfect.buckaroo.RecipeVersion;
import com.loopperfect.buckaroo.Resource;

import java.lang.reflect.Type;
import java.util.Optional;

public final class RecipeVersionDeserializer implements JsonDeserializer<RecipeVersion> {

    @Override
    public RecipeVersion deserialize(
            final JsonElement jsonElement, final Type type, final JsonDeserializationContext context)
            throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final GitCommit url = context.deserialize(jsonObject.get("url"), GitCommit.class);

        final Optional<String> target = jsonObject.has("target") ?
                Optional.of(jsonObject.get("target").getAsString()) :
                Optional.empty();

        final DependencyGroup dependencies = jsonObject.has("dependencies") ?
                context.deserialize(jsonObject.get("dependencies"), DependencyGroup.class) :
                DependencyGroup.of();

        final Optional<Resource> buckResource = jsonObject.has("buck") ?
                Optional.of(context.deserialize(jsonObject.get("buck"), Resource.class)) :
                Optional.empty();

        return RecipeVersion.of(url, target, dependencies, buckResource);
    }
}
