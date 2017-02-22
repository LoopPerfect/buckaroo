package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.*;
import com.loopperfect.buckaroo.BuckarooConfig;
import com.loopperfect.buckaroo.RemoteCookBook;

import java.lang.reflect.Type;
import java.util.stream.Collectors;

public final class BuckarooConfigDeserializer implements JsonDeserializer<BuckarooConfig> {

    @Override
    public BuckarooConfig deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final JsonArray cookBooksElement = jsonObject.getAsJsonArray("cookBooks");


        if (cookBooksElement == null)  // TODO: Return Either
            return BuckarooConfig.of(ImmutableList.of());

        final ImmutableList<RemoteCookBook> cookBooks = ImmutableList.copyOf(
            Streams.stream(cookBooksElement)
                .map(x -> (RemoteCookBook) context.deserialize(x, RemoteCookBook.class))
                .collect(Collectors.toList()));

        return BuckarooConfig.of(cookBooks);
    }
}
