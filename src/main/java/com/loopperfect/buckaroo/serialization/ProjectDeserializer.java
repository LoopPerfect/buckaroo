package com.loopperfect.buckaroo.serialization;

import com.google.gson.*;
import com.loopperfect.buckaroo.DependencyGroup;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.Project;

import java.lang.reflect.Type;
import java.util.Optional;

public final class ProjectDeserializer implements JsonDeserializer<Project> {

    @Override
    public Project deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final Optional<String> name = jsonObject.has("name") ?
            Optional.of(jsonObject.get("name").getAsString()) :
            Optional.empty();

        final Optional<String> license = jsonObject.has("license") ?
            Optional.of(jsonObject.get("license").getAsString()) :
            Optional.empty();

        DependencyGroup dependencies;
        if (jsonObject.has("dependencies")) {
            dependencies = context.deserialize(jsonObject.get("dependencies"), DependencyGroup.class);
        } else {
            dependencies = DependencyGroup.of();
        }

        return Project.of(name, license, dependencies);
    }
}
