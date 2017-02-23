package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;

import java.util.Optional;

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
                    Optional.empty(),
                    "my-magic-lib",
                    DependencyGroup.of(
                        ImmutableMap.of(
                            Identifier.of("awesome"), AnySemanticVersion.of()))),
                SemanticVersion.of(1, 1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/magicco/magiclib/commit", "c7355d5"),
                    "my-magic-lib")));

        final String serializedRecipe = Serializers.serialize(recipe);

        final Either<JsonParseException, Recipe> deserializedRecipe =
                Serializers.parseRecipe(serializedRecipe);

        assertEquals(Either.right(recipe), deserializedRecipe);
    }
}
