package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.RecipeIdentifier;

import java.lang.reflect.Type;

public final class RecipeIdentifierDeserializer implements JsonDeserializer<RecipeIdentifier> {

    @Override
    public RecipeIdentifier deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final String string = jsonElement.getAsString();

        return RecipeIdentifier.parse(string)
                .orElseThrow(() -> new JsonParseException("\"" + string + "\" is not a valid identifier"));
    }
}
