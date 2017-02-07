package com.loopperfect.buckaroo.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopperfect.buckaroo.*;

public final class Serializers {

    private Serializers() {

    }

    public static Gson gson() {

        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(SemanticVersion.class, new SemanticVersionSerializer());
        gsonBuilder.registerTypeAdapter(SemanticVersion.class, new SemanticVersionDeserializer());

        gsonBuilder.registerTypeAdapter(SemanticVersionRequirement.class, new SemanticVersionRequirementSerializer());
        gsonBuilder.registerTypeAdapter(SemanticVersionRequirement.class, new SemanticVersionRequirementDeserializer());

        gsonBuilder.registerTypeAdapter(Identifier.class, new IdentifierSerializer());
        gsonBuilder.registerTypeAdapter(Identifier.class, new IdentifierDeserializer());

        gsonBuilder.registerTypeAdapter(Dependency.class, new DependencySerializer());
        gsonBuilder.registerTypeAdapter(Dependency.class, new DependencyDeserializer());

        gsonBuilder.registerTypeAdapter(RecipeVersion.class, new RecipeVersionSerializer());
        gsonBuilder.registerTypeAdapter(RecipeVersion.class, new RecipeVersionDeserializer());

        gsonBuilder.registerTypeAdapter(Recipe.class, new RecipeSerializer());
        gsonBuilder.registerTypeAdapter(Recipe.class, new RecipeDeserializer());

        gsonBuilder.registerTypeAdapter(Project.class, new ProjectSerializer());
        gsonBuilder.registerTypeAdapter(Project.class, new ProjectDeserializer());

        return gsonBuilder.create();
    }
}
