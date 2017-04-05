package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public final class HashCodeDeserializer implements JsonDeserializer<HashCode> {

    @Override
    public HashCode deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        try {
            return HashCode.fromString(jsonElement.getAsString());
        } catch (final IllegalArgumentException e) {
            throw new JsonParseException(e);
        }
    }
}
