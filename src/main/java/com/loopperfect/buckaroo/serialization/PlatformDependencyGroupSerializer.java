package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.loopperfect.buckaroo.DependencyGroup;
import com.loopperfect.buckaroo.PlatformDependencyGroup;
import org.javatuples.Pair;

import java.lang.reflect.Type;

public final class PlatformDependencyGroupSerializer implements JsonSerializer<PlatformDependencyGroup> {

    @Override
    public JsonElement serialize(final PlatformDependencyGroup platformDependencyGroup, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(platformDependencyGroup);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonArray jsonArray = new JsonArray();

        for (final Pair<String, DependencyGroup> i : platformDependencyGroup.entries()) {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("platform", i.getValue0());
            jsonObject.add("dependencies", context.serialize(i.getValue1()));
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }
}
