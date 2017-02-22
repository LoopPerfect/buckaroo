package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.Dependency;
import com.loopperfect.buckaroo.DependencyGroup;

import java.lang.reflect.Type;

public final class DependencyGroupSerializer implements JsonSerializer<DependencyGroup> {

    @Override
    public JsonElement serialize(final DependencyGroup dependencyGroup, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(dependencyGroup);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        for (final Dependency i : dependencyGroup.entries()) {
            jsonObject.addProperty(i.project.name, i.versionRequirement.encode());
        }

        return jsonObject;
    }
}
