package com.loopperfect.buckaroo.github.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.github.GitHubRelease;

import java.lang.reflect.Type;

public final class GitHubReleaseSerializer implements JsonSerializer<GitHubRelease> {

    @Override
    public JsonElement serialize(final GitHubRelease gitHubRelease, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(gitHubRelease);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", gitHubRelease.name);
        jsonObject.addProperty("id", gitHubRelease.id);
        jsonObject.addProperty("tag_name", gitHubRelease.tagName);
        jsonObject.addProperty("zipball_url", gitHubRelease.zipURL.toExternalForm());

        return jsonObject;
    }
}
