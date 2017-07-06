package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.DependencyLock;
import com.loopperfect.buckaroo.DependencyLocks;

import java.lang.reflect.Type;

public final class DependencyLocksSerializer implements JsonSerializer<DependencyLocks> {

    @Override
    public JsonElement serialize(final DependencyLocks dependencyLocks, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(dependencyLocks);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        for (final DependencyLock dependencyLock : dependencyLocks.entries()) {
            jsonObject.add(
                dependencyLock.identifier.encode(),
                context.serialize(dependencyLock.origin));
        }

        return jsonObject;
    }
}
