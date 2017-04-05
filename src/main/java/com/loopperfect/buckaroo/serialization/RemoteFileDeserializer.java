package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.gson.*;
import com.loopperfect.buckaroo.RemoteFile;

import java.lang.reflect.Type;
import java.net.URL;

public final class RemoteFileDeserializer implements JsonDeserializer<RemoteFile> {

    @Override
    public RemoteFile deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final URL url = context.deserialize(jsonObject.get("url"), URL.class);
        final HashCode sha256 = context.deserialize(jsonObject.get("sha256"), HashCode.class);

        return RemoteFile.of(url, sha256);
    }
}
