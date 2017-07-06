package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.RemoteCookbook;

import java.lang.reflect.Type;

public final class RemoteCookBookSerializer implements JsonSerializer<RemoteCookbook> {

    @Override
    public JsonElement serialize(final RemoteCookbook cookBook, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(cookBook);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.add("name", context.serialize(cookBook.name));
        jsonObject.add("url", context.serialize(cookBook.url));

        return jsonObject;
    }
}
