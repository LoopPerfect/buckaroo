package com.loopperfect.buckaroo.serialization;

import com.google.gson.*;
import com.loopperfect.buckaroo.DependencyGroup;
import com.loopperfect.buckaroo.PlatformDependencyGroup;
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

        final Optional<String> target = jsonObject.has("target") ?
            Optional.of(jsonObject.get("target").getAsString()) :
            Optional.empty();

        final Optional<String> license = jsonObject.has("license") ?
            Optional.of(jsonObject.get("license").getAsString()) :
            Optional.empty();

        final DependencyGroup dependencies = jsonObject.has("dependencies") ?
            context.deserialize(jsonObject.get("dependencies"), DependencyGroup.class) :
            DependencyGroup.of();

        final PlatformDependencyGroup platformDependencies = jsonObject.has("platformDependencies") ?
            context.deserialize(jsonObject.get("platformDependencies"), PlatformDependencyGroup.class) :
            PlatformDependencyGroup.of();

        return Project.of(name, target, license, dependencies, platformDependencies);
    }
}
