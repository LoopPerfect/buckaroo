package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.DependencyGroup;
import com.loopperfect.buckaroo.PlatformDependencyGroup;
import org.javatuples.Pair;

import java.lang.reflect.Type;

public final class PlatformDependencyGroupDeserializer implements JsonDeserializer<PlatformDependencyGroup> {

    @Override
    public PlatformDependencyGroup deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final ImmutableList<Pair<String, DependencyGroup>> platformDependencies = Streams.stream(jsonElement.getAsJsonArray())
            .map(JsonElement::getAsJsonObject)
            .map(x -> Pair.with(
                x.getAsJsonPrimitive("platform").getAsString(),
                (DependencyGroup)context.deserialize(x.get("dependencies"), DependencyGroup.class)))
            .collect(ImmutableList.toImmutableList());

        return PlatformDependencyGroup.of(platformDependencies);
    }
}
