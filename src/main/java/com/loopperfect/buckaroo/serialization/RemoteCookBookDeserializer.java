package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RemoteCookBook;

import java.lang.reflect.Type;

public final class RemoteCookBookDeserializer implements JsonDeserializer<RemoteCookBook> {

    @Override
    public RemoteCookBook deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final Identifier name = context.deserialize(jsonObject.get("name"), Identifier.class);
        final String url = jsonObject.get("url").getAsString();

        return RemoteCookBook.of(name, url);
    }
}
