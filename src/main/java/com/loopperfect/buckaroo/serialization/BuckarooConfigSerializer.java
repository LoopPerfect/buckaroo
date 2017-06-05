package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.loopperfect.buckaroo.BuckarooConfig;
import com.loopperfect.buckaroo.RemoteCookbook;

import java.lang.reflect.Type;

public final class BuckarooConfigSerializer implements JsonSerializer<BuckarooConfig> {

    @Override
    public JsonElement serialize(final BuckarooConfig buckarooConfig, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(buckarooConfig);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        final JsonArray cookBooksJsonArray = new JsonArray();

        for (final RemoteCookbook cookBook : buckarooConfig.cookbooks) {
            cookBooksJsonArray.add(context.serialize(cookBook));
        }

        jsonObject.add("cookbooks", cookBooksJsonArray);

        if (buckarooConfig.analyticsServer.isPresent()) {
            jsonObject.addProperty("analytics", buckarooConfig.analyticsServer.get().toExternalForm());
        }

        return jsonObject;
    }
}
