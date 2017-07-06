package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.loopperfect.buckaroo.*;

import java.lang.reflect.Type;

public final class DependencyLocksDeserializer implements JsonDeserializer<DependencyLocks> {

    @Override
    public DependencyLocks deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final ImmutableMap<RecipeIdentifier, ResolvedDependency> locks = jsonElement.getAsJsonObject()
            .entrySet()
            .stream()
            .collect(ImmutableMap.toImmutableMap(
                x -> context.deserialize(new JsonPrimitive(x.getKey()), RecipeIdentifier.class),
                x -> context.deserialize(x.getValue(), ResolvedDependency.class)));

        return DependencyLocks.of(locks);
    }
}
