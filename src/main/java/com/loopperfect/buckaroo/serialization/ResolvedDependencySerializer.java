package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.RemoteFile;
import com.loopperfect.buckaroo.ResolvedDependency;

import java.lang.reflect.Type;

public final class ResolvedDependencySerializer implements JsonSerializer<ResolvedDependency> {

    @Override
    public JsonElement serialize(
            final ResolvedDependency resolvedDependency, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(resolvedDependency);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        final JsonElement sourceElement = Either.join(
            resolvedDependency.source,
            context::serialize,
            context::serialize);

        jsonObject.add("source", sourceElement);

        if (resolvedDependency.target.isPresent()) {
            jsonObject.addProperty("target", resolvedDependency.target.get());
        }

        if (!resolvedDependency.dependencies.isEmpty()) {
            jsonObject.add("dependencies", context.serialize(resolvedDependency.dependencies));
        }

        if (resolvedDependency.buckResource.isPresent()) {
            jsonObject.add("buck", context.serialize(resolvedDependency.buckResource.get(), RemoteFile.class));
        }

        return jsonObject;
    }
}
