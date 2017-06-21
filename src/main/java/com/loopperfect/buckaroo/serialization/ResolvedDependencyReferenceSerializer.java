package com.loopperfect.buckaroo.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.ResolvedDependencyReference;

import java.lang.reflect.Type;

public final class ResolvedDependencyReferenceSerializer implements JsonSerializer<ResolvedDependencyReference> {

    @Override
    public JsonElement serialize(final ResolvedDependencyReference src, final Type type, final JsonSerializationContext context) {
        return new JsonPrimitive(src.encode());
    }
}
