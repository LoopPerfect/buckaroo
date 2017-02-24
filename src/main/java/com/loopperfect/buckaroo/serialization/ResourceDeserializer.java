package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.loopperfect.buckaroo.FileResource;
import com.loopperfect.buckaroo.Resource;
import com.loopperfect.buckaroo.UrlResource;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;

public final class ResourceDeserializer implements JsonDeserializer<Resource> {

    @Override
    public Resource deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        if (jsonObject.has("path")) {
            final String path = jsonObject.get("path").getAsString();
            return FileResource.of(path);
        } else {
            try {
                final URL url = new URL(jsonObject.get("url").getAsString());
                return UrlResource.of(url);
            } catch (final MalformedURLException e) {
                throw new JsonParseException(e);
            }
        }
    }
}
