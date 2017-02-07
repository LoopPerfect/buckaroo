package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.Dependency;

import java.lang.reflect.Type;

public final class DependencySerializer implements JsonSerializer<Dependency> {

    @Override
    public JsonElement serialize(final Dependency dependency, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(dependency);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("project", dependency.project.toString());
        jsonObject.add("version", context.serialize(dependency.versionRequirement));

        return jsonObject;
    }
}
