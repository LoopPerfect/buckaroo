package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.*;

import java.lang.reflect.Type;

public final class RecipeVersionUnlockedSerializer implements JsonSerializer<RecipeVersionUnlocked> {

    @Override
    public JsonElement serialize(
        final RecipeVersionUnlocked recipeVersion, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(recipeVersion);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        final JsonElement sourceElement = Either.join(
            recipeVersion.source,
            context::serialize,
            context::serialize);

        jsonObject.add("source", sourceElement);

        if (recipeVersion.target.isPresent()) {
            jsonObject.addProperty("target", recipeVersion.target.get());
        }

        if (recipeVersion.dependencies.map(DependencyGroup::any).orElse(false)) {
            jsonObject.add("dependencies", context.serialize(recipeVersion.dependencies.get()));
        }

        if (recipeVersion.buckResource.isPresent()) {
            jsonObject.addProperty("buck", recipeVersion.buckResource.get().toString());
        }

        return jsonObject;
    }
}
