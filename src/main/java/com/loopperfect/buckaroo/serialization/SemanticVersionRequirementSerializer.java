package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.SemanticVersionRequirement;

import java.lang.reflect.Type;

public final class SemanticVersionRequirementSerializer implements JsonSerializer<SemanticVersionRequirement> {

    @Override
    public JsonElement serialize(final SemanticVersionRequirement versionRequirement, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(versionRequirement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        return new JsonPrimitive(versionRequirement.encode());
    }
}
