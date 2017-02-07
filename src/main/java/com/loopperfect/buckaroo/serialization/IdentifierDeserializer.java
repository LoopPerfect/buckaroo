package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.Identifier;

import java.lang.reflect.Type;

public final class IdentifierDeserializer implements JsonDeserializer<Identifier> {

    @Override
    public Identifier deserialize(final JsonElement jsonElement, final Type type, JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        if (!jsonElement.isJsonPrimitive()) {
            throw new JsonParseException("Expected a string");
        }

        return Identifier.parse(jsonElement.getAsString())
                .orElseThrow(() -> new JsonParseException("\"" + jsonElement.getAsString() + "\" is not a valid name"));
    }
}
