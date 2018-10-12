package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.loopperfect.buckaroo.RemoteArchiveUnlocked;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Optional;

public final class RemoteArchiveUnlockedDeserializer implements JsonDeserializer<RemoteArchiveUnlocked> {

    @Override
    public RemoteArchiveUnlocked deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (!jsonObject.has("url")) {
            throw new JsonParseException("A remote archive must have a URL");
        }

        final URI url = context.deserialize(jsonObject.get("url"), URI.class);
        final Optional<String> subPath = jsonObject.has("subPath") ?
            Optional.of(jsonObject.get("subPath").getAsString()) :
            Optional.empty();

        return RemoteArchiveUnlocked.of(url, subPath);
    }
}
