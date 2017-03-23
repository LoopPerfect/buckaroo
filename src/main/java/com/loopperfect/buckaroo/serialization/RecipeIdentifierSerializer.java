package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.RecipeIdentifier;

import java.lang.reflect.Type;

public final class RecipeIdentifierSerializer implements JsonSerializer<RecipeIdentifier> {

    @Override
    public JsonElement serialize(final RecipeIdentifier identifier, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        return new JsonPrimitive(identifier.encode());
    }
}
