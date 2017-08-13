package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.gson.*;
import com.loopperfect.buckaroo.RemoteArchive;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

public final class RemoteArchiveDeserializer implements JsonDeserializer<RemoteArchive> {

    @Override
    public RemoteArchive deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (!jsonObject.has("url")) {
            throw new JsonParseException("A remote archive must have a URL");
        }

        final URI url = context.deserialize(jsonObject.get("url"), URI.class);
        final HashCode sha256 = context.deserialize(jsonObject.get("sha256"), HashCode.class);
        final Optional<String> subPath = jsonObject.has("subPath") ?
            Optional.of(jsonObject.get("subPath").getAsString()) :
            Optional.empty();

        return RemoteArchive.of(url, sha256, subPath);
    }
}
