package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.FileResource;

import java.lang.reflect.Type;

public final class FileResourceSerializer implements JsonSerializer<FileResource> {

    @Override
    public JsonElement serialize(final FileResource resource, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(resource);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("path", resource.path);

        return jsonObject;
    }
}
