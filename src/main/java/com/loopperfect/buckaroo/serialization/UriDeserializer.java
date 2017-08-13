package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;

public final class UriDeserializer implements JsonDeserializer<URI> {

    @Override
    public URI deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        if (jsonElement.getAsString() == null) {
            throw new JsonParseException("URI must be a string");
        }

        try {
            return new URI(jsonElement.getAsString());
        } catch (final URISyntaxException e) {
            throw new JsonParseException(e);
        }
    }
}
