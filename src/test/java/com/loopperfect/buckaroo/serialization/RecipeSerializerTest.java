package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.crypto.Hash;
import com.loopperfect.buckaroo.versioning.WildcardVersion;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public final class RecipeSerializerTest {

    @Test
    public void test1() throws Exception {

        final Recipe recipe = Recipe.of(
            "magic-lib",
            new URI("https://github.com/magicco/magiclib"),
            ImmutableMap.of(
                SemanticVersion.of(1, 0),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/magicco/magiclib/commit", "b0215d5"),
                    Optional.of("my-magic-lib"),
                    DependencyGroup.of(
                        ImmutableMap.of(
                            RecipeIdentifier.of("org", "awesome"),
                            WildcardVersion.of())),
                    Optional.empty()),
                SemanticVersion.of(1, 1),
                RecipeVersion.of(
                    GitCommit.of("https://github.com/magicco/magiclib/commit", "c7355d5"),
                    "my-magic-lib")));

        final String serializedRecipe = Serializers.serialize(recipe);

        final Either<JsonParseException, Recipe> deserializedRecipe =
                Serializers.parseRecipe(serializedRecipe);

        assertEquals(Either.right(recipe), deserializedRecipe);
    }

    @Test
    public void test2() throws Exception {

        final Recipe recipe = Recipe.of(
            "magic-lib",
            new URI("https://github.com/magicco/magiclib"),
            ImmutableMap.of(
                SemanticVersion.of(1, 0),
                RecipeVersion.of(
                    RemoteArchive.of(
                        new URI("https://github.com/magicco/1.0.0/magiclib.zip"),
                        Hash.sha256("Hello, world. ")),
                    Optional.of("my-magic-lib"),
                    DependencyGroup.of(
                        ImmutableMap.of(
                            RecipeIdentifier.of("org", "awesome"),
                            WildcardVersion.of())),
                    Optional.of(RemoteFile.of(
                        new URI("https://github.com/magicco/1.0.0/magiclib.zip"),
                        Hash.sha256("Hello, world. ")))),
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
