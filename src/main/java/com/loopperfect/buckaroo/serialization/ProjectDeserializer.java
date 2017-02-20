package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.loopperfect.buckaroo.*;

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

        ImmutableMap<Identifier, SemanticVersionRequirement> dependencies;
        if (jsonObject.has("dependencies")) {
            final JsonObject jsonObjectDependencies = jsonObject.getAsJsonObject("dependencies");
            dependencies = ImmutableMap.copyOf(
                    jsonObjectDependencies.entrySet().stream().collect(Collectors.toMap(
                            x -> context.deserialize(new JsonPrimitive(x.getKey()), Identifier.class),
                            x -> context.deserialize(x.getValue(), SemanticVersionRequirement.class))));
        } else {
            dependencies = ImmutableMap.of();
        }

        return Project.of(name, license, DependencyGroup.of(dependencies));
    }
}
