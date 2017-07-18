package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.*;

import java.lang.reflect.Type;

public final class RecipeVersionSerializer implements JsonSerializer<RecipeVersion> {

    @Override
    public JsonElement serialize(
            final RecipeVersion recipeVersion, final Type type, final JsonSerializationContext context) {

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

        if (recipeVersion.platformDependencies.map(PlatformDependencyGroup::any).orElse(false)) {
            jsonObject.add("platformDependencies", context.serialize(recipeVersion.platformDependencies.get()));
        }

        if (recipeVersion.buckResource.isPresent()) {
            jsonObject.add("buck", context.serialize(recipeVersion.buckResource.get(), RemoteFile.class));
        }

        return jsonObject;
    }
}
