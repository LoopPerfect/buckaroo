package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.Recipe;

import java.lang.reflect.Type;

public final class RecipeSerializer implements JsonSerializer<Recipe> {

    @Override
    public JsonElement serialize(final Recipe recipe, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(recipe);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.add("name", context.serialize(recipe.name));
        jsonObject.addProperty("url", recipe.url.toString());
        jsonObject.add("versions", context.serialize(recipe.versions));

        return jsonObject;
    }
}
