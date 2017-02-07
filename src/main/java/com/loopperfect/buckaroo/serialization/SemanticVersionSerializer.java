package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.SemanticVersion;

import java.lang.reflect.Type;

public final class SemanticVersionSerializer implements JsonSerializer<SemanticVersion> {

    @Override
    public JsonElement serialize(final SemanticVersion semanticVersion, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(semanticVersion);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        return new JsonPrimitive(semanticVersion.toString());
    }
}
