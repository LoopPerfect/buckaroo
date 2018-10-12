package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.RemoteArchiveUnlocked;

import java.lang.reflect.Type;

public final class RemoteArchiveUnlockedSerializer implements JsonSerializer<RemoteArchiveUnlocked> {

    @Override
    public JsonElement serialize(final RemoteArchiveUnlocked remoteArchive, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(remoteArchive);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("url", remoteArchive.url.toString());

        if (remoteArchive.subPath.isPresent()) {
            jsonObject.addProperty("subPath", remoteArchive.subPath.get());
        }

        return jsonObject;
    }
}
