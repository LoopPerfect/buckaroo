package com.loopperfect.buckaroo.serialization;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.SemanticVersionRequirement;
import com.loopperfect.buckaroo.versioning.VersioningParsers;
import org.jparsec.error.ParserException;

import java.lang.reflect.Type;

public final class SemanticVersionRequirementDeserializer implements JsonDeserializer<SemanticVersionRequirement> {

    @Override
    public SemanticVersionRequirement deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        try {
            return VersioningParsers.semanticVersionRequirementParser
                    .parse(jsonElement.getAsString());
        } catch (final ParserException e) {
            throw new JsonParseException("Invalid version", e);
        }
    }
}
