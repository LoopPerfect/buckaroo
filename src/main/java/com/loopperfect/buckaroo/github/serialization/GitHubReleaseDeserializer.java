package com.loopperfect.buckaroo.github.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.loopperfect.buckaroo.github.GitHubRelease;

import java.lang.reflect.Type;
import java.net.URL;

public final class GitHubReleaseDeserializer implements JsonDeserializer<GitHubRelease> {

    @Override
    public GitHubRelease deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(jsonElement);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final String name = jsonObject.getAsJsonPrimitive("name").getAsString();
        final int id = jsonObject.getAsJsonPrimitive("id").getAsInt();
        final String tagName = jsonObject.getAsJsonPrimitive("tag_name").getAsString();
        final URL zipURL = context.deserialize(jsonObject.get("zipball_url"), URL.class);

        return GitHubRelease.of(name, id, tagName, zipURL);
    }
}
