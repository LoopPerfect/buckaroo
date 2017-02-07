package com.loopperfect.buckaroo.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.Identifier;

import java.lang.reflect.Type;

public final class IdentifierSerializer implements JsonSerializer<Identifier> {

    @Override
    public JsonElement serialize(final Identifier identifier, final Type type, final JsonSerializationContext context) {
        return new JsonPrimitive(identifier.name);
    }
}
