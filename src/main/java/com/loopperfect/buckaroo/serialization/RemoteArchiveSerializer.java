package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.RemoteArchive;

import java.lang.reflect.Type;

public final class RemoteArchiveSerializer implements JsonSerializer<RemoteArchive> {

    @Override
    public JsonElement serialize(final RemoteArchive remoteArchive, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(remoteArchive);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("url", remoteArchive.url.toString());
        jsonObject.addProperty("sha256", remoteArchive.sha256.toString());

        if (remoteArchive.subPath.isPresent()) {
            jsonObject.addProperty("subPath", remoteArchive.subPath.get());
        }

        return jsonObject;
    }
}
