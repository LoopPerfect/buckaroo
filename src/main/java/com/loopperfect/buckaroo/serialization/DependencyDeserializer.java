package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.loopperfect.buckaroo.Dependency;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.SemanticVersionRequirement;
import com.loopperfect.buckaroo.SemanticVersionRequirements;

import java.lang.reflect.Type;
import java.util.Optional;

public final class DependencyDeserializer implements JsonDeserializer<Dependency> {

    @Override
    public Dependency deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final String projectString = jsonObject.get("project").getAsString();

        final Identifier project = Identifier.parse(projectString)
                .orElseThrow(() -> new JsonParseException("\"" + projectString + "\" is not a valid identifier"));

        final String versionRequirementString = jsonObject.get("version").getAsString();

        final SemanticVersionRequirement versionRequirement = SemanticVersionRequirements.parse(versionRequirementString)
                .orElseThrow(() -> new JsonParseException("\"" + versionRequirementString + "\" is not a valid version"));

        return Dependency.of(project, versionRequirement);
    }
}
