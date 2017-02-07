package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.Project;

import java.lang.reflect.Type;

public final class ProjectSerializer implements JsonSerializer<Project> {

    @Override
    public JsonElement serialize(final Project project, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", project.name.name);

        if (project.license.isPresent()) {
            jsonObject.addProperty("license", project.license.get().toString());
        }

        final JsonElement dependenciesElement = context.serialize(project.dependencies);
        jsonObject.add("dependencies", dependenciesElement);

        return jsonObject;
    }
}
