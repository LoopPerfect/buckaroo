package com.loopperfect.buckaroo.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopperfect.buckaroo.Project;
import com.loopperfect.buckaroo.SemanticVersionRequirement;

public final class Serializers {

    private Serializers() {

    }

    public static Gson gson() {

        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(SemanticVersionRequirement.class, new SemanticVersionRequirementSerializer());
        gsonBuilder.registerTypeAdapter(SemanticVersionRequirement.class, new SemanticVersionRequirementDeserializer());

        gsonBuilder.registerTypeAdapter(Project.class, new ProjectSerializer());
        gsonBuilder.registerTypeAdapter(Project.class, new ProjectDeserializer());

        return gsonBuilder.create();
    }
}
