package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.SemanticVersion;

import java.lang.reflect.Type;

public final class SemanticVersionDeserializer implements JsonDeserializer<SemanticVersion> {

    @Override
    public SemanticVersion deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        return SemanticVersion.parse(jsonElement.getAsString())
            .orElseThrow(() -> new JsonParseException("\"" + jsonElement.getAsString() + "\" is not a valid verison"));
    }
}
