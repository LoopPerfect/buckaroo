package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.*;

import java.lang.reflect.Type;
import java.util.Map;

public final class OrganizationSerializer implements JsonSerializer<Organization> {

    @Override
    public JsonElement serialize(final Organization organization, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(organization);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", organization.name);

        final JsonObject recipesJsonObject = new JsonObject();

        for (final Map.Entry<Identifier, Recipe> i : organization.recipes.entrySet()) {
            recipesJsonObject.add(i.getKey().name, context.serialize(i.getValue()));
        }

        jsonObject.add("recipes", recipesJsonObject);

        return jsonObject;
    }
}
