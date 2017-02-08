package com.loopperfect.buckaroo.serialization;

import com.google.gson.*;
import com.loopperfect.buckaroo.GitCommit;
import com.loopperfect.buckaroo.RecipeVersion;

import java.lang.reflect.Type;
import java.util.Optional;

public final class RecipeVersionDeserializer implements JsonDeserializer<RecipeVersion> {

    @Override
    public RecipeVersion deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

//        final String url = jsonObject.get("url").getAsString();
        final GitCommit url = context.deserialize(jsonObject.get("url"), GitCommit.class);

        Optional<String> buckUrl;
        if (jsonObject.has("buck-url")) {
            buckUrl = Optional.of(jsonObject.get("buck-url").getAsString());
        } else {
            buckUrl = Optional.empty();
        }

        final String target = jsonObject.get("target").getAsString();

        return RecipeVersion.of(url, buckUrl, target);
    }
}
