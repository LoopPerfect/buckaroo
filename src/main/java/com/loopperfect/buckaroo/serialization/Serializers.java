package com.loopperfect.buckaroo.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopperfect.buckaroo.*;

public final class Serializers {

    private Serializers() {

    }

    public static Gson gson(final boolean usePrettyPrinting) {

        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(SemanticVersion.class, new SemanticVersionSerializer());
        gsonBuilder.registerTypeAdapter(SemanticVersion.class, new SemanticVersionDeserializer());

        gsonBuilder.registerTypeAdapter(GitCommit.class, new GitCommitSerializer());
        gsonBuilder.registerTypeAdapter(GitCommit.class, new GitCommitDeserializer());

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

        gsonBuilder.registerTypeAdapter(BuckarooConfig.class, new BuckarooConfigSerializer());
        gsonBuilder.registerTypeAdapter(BuckarooConfig.class, new BuckarooConfigDeserializer());

        gsonBuilder.registerTypeAdapter(RemoteCookBook.class, new RemoteCookBookSerializer());
        gsonBuilder.registerTypeAdapter(RemoteCookBook.class, new RemoteCookBookDeserializer());

        if (usePrettyPrinting) {
            gsonBuilder.setPrettyPrinting();
        }

        return gsonBuilder.create();
    }

    public static Gson gson() {
        return gson(false);
    }
    }
