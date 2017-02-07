package com.loopperfect.buckaroo.serialization;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.SemanticVersionRequirement;
import com.loopperfect.buckaroo.SemanticVersionRequirements;

import java.lang.reflect.Type;

public final class SemanticVersionRequirementDeserializer implements JsonDeserializer<SemanticVersionRequirement> {

    @Override
    public SemanticVersionRequirement deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        return SemanticVersionRequirements.parse(jsonElement.getAsString())
                .orElseThrow(() -> new JsonParseException("\"" + jsonElement.getAsString() + "\" is not a valid version string"));
    }
}
