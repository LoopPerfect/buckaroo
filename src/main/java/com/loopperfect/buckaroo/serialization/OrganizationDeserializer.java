package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.loopperfect.buckaroo.*;

import java.lang.reflect.Type;

public final class OrganizationDeserializer implements JsonDeserializer<Organization> {

    @Override
    public Organization deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final String name = jsonObject.get("name").getAsString();

        final JsonObject recipesJsonObject = jsonObject.get("recipes").getAsJsonObject();

        final ImmutableMap<Identifier, Recipe> recipes = recipesJsonObject.entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(
                    x -> context.deserialize(new JsonPrimitive(x.getKey()), Identifier.class),
                    x -> context.deserialize(x.getValue(), Organization.class)));

        return Organization.of(name, recipes);
    }
}
