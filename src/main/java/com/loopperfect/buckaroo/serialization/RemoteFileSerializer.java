package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.RemoteFile;

import java.lang.reflect.Type;

public final class RemoteFileSerializer implements JsonSerializer<RemoteFile> {

    @Override
    public JsonElement serialize(final RemoteFile remoteFile, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(remoteFile);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("url", remoteFile.url.toString());
        jsonObject.addProperty("sha256", remoteFile.sha256.toString());

        return jsonObject;
    }
}
