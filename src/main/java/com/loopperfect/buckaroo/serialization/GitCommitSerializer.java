package com.loopperfect.buckaroo.serialization;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.loopperfect.buckaroo.GitCommit;

import java.lang.reflect.Type;

public final class GitCommitSerializer implements JsonSerializer<GitCommit> {

    @Override
    public JsonElement serialize(final GitCommit gitCommit, final Type type, final JsonSerializationContext context) {

        Preconditions.checkNotNull(gitCommit);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        return new JsonPrimitive(gitCommit.toString());
    }
}
