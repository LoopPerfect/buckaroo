package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.UrlResource;

import java.lang.reflect.Type;

public final class UrlResourceSerializer implements JsonSerializer<UrlResource> {

    @Override
    public JsonElement serialize(final UrlResource urlResource, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(urlResource);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("url", urlResource.url.toExternalForm());

        return jsonObject;
    }
}
