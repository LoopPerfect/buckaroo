package com.loopperfect.buckaroo.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopperfect.buckaroo.Project;

public final class Serializers {

    private Serializers() {

    }

    public static Gson gson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Project.class, new ProjectSerializer());
        return gsonBuilder.create();
    }
}
