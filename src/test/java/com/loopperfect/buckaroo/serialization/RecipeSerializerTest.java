package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.loopperfect.buckaroo.*;

import static org.junit.Assert.assertEquals;

public final class RecipeSerializerTest {

    @org.junit.Test
    public void testRecipeSerialization() {

        final Recipe recipe = Recipe.of(
                                Identifier.of("magic-lib"),
                                "https://github.com/magicco/magiclib/commit/b0215d5",
                                ImmutableMap.of(
                                        SemanticVersion.of(1, 0),
                                        RecipeVersion.of(
                                                GitCommit.of("https://github.com/magicco/magiclib/commit", "b0215d5"),
                                                "my-magic-lib"),
                                        SemanticVersion.of(1, 1),
                                        RecipeVersion.of(
                                                GitCommit.of("https://github.com/magicco/magiclib/commit", "c7355d5"),
                                                "my-magic-lib")));

        final Gson gson = Serializers.gson();

        final String serializedRecipe = gson.toJson(recipe);

        final Recipe deserializedRecipe = gson.fromJson(serializedRecipe, Recipe.class);

        assertEquals(recipe, deserializedRecipe);
    }
}
