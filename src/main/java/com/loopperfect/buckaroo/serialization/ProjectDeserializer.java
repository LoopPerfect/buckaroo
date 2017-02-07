package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.*;
import com.loopperfect.buckaroo.Dependency;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.Project;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ProjectDeserializer implements JsonDeserializer<Project> {

    @Override
    public Project deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final Identifier name = context.deserialize(jsonObject.get("name"), Identifier.class);

        Optional<String> license;
        if (jsonObject.has("license")) {
            license = Optional.of(jsonObject.get("license").getAsString());
        } else {
            license = Optional.empty();
        }

        ImmutableSet<Dependency> dependencies;
        if (jsonObject.has("dependencies")) {
            dependencies = ImmutableSet.copyOf(Streams.stream(jsonObject.get("dependencies").getAsJsonArray())
                    .map(x -> (Dependency)context.deserialize(x, Dependency.class))
                    .collect(Collectors.toSet()));
        } else {
            dependencies = ImmutableSet.of();
        }

        return new Project(name, license, dependencies);
    }
}
