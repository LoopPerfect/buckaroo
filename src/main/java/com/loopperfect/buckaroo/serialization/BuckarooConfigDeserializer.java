package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.*;
import com.loopperfect.buckaroo.BuckarooConfig;
import com.loopperfect.buckaroo.RemoteCookbook;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Collectors;

public final class BuckarooConfigDeserializer implements JsonDeserializer<BuckarooConfig> {

    @Override
    public BuckarooConfig deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final JsonArray cookBooksElement = jsonObject.getAsJsonArray("cookbooks");

        final ImmutableList<RemoteCookbook> cookBooks = ImmutableList.copyOf(
            Streams.stream(cookBooksElement == null ? ImmutableList.of() : cookBooksElement)
                .map(x -> (RemoteCookbook) context.deserialize(x, RemoteCookbook.class))
                .collect(Collectors.toList()));

        final Optional<URL> analyticsServer = jsonObject.has("analytics") ?
            Optional.of(context.deserialize(jsonObject.get("analytics"), URL.class)) :
            Optional.empty();

        return BuckarooConfig.of(cookBooks, analyticsServer);
    }
}
