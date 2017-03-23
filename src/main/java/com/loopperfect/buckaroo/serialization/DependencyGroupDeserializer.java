package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.loopperfect.buckaroo.DependencyGroup;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.SemanticVersionRequirement;

import java.lang.reflect.Type;

public final class DependencyGroupDeserializer implements JsonDeserializer<DependencyGroup> {

    @Override
    public DependencyGroup deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        return DependencyGroup.of(
            jsonObject.entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(
                    x -> context.deserialize(new JsonPrimitive(x.getKey()), RecipeIdentifier.class),
                    x -> context.deserialize(x.getValue(), SemanticVersionRequirement.class))));
    }
}
