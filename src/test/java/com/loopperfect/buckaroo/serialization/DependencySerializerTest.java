package com.loopperfect.buckaroo.serialization;

import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import com.loopperfect.buckaroo.versioning.BoundedSemanticVersion;
import com.loopperfect.buckaroo.versioning.ExactSemanticVersion;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class DependencySerializerTest {

    private static void serializeDeserialize(final Dependency dependency) {

        final String serializedDependency = Serializers.serialize(dependency);

        final Either<JsonParseException, Dependency> deserizializedDependency =
                Serializers.parseDependency(serializedDependency);

        assertEquals(Either.right(dependency), deserizializedDependency);
    }

    @Test
    public void testDependencySerializer1() {

        serializeDeserialize(Dependency.of(
            RecipeIdentifier.of("org", "magic-libs"),
            ExactSemanticVersion.of(SemanticVersion.of(3, 4))));
    }

    @Test
    public void testDependencySerializer2() {

        serializeDeserialize(Dependency.of(
            RecipeIdentifier.of("org", "magic-libs"),
            ExactSemanticVersion.of(SemanticVersion.of(3, 4), SemanticVersion.of(6, 5, 1))));
    }

    @Test
    public void testDependencySerializer3() {

        serializeDeserialize(Dependency.of(
            RecipeIdentifier.of("bigco", "magic-libs"),
            AnySemanticVersion.of()));
    }

    @Test
    public void testDependencySerializer4() {

        serializeDeserialize(Dependency.of(
            RecipeIdentifier.of("org", "magic-libs"),
            BoundedSemanticVersion.of(SemanticVersion.of(4), AboveOrBelow.ABOVE)));
    }

    @Test
    public void testDependencySerializer5() {

        serializeDeserialize(Dependency.of(
            RecipeIdentifier.of("g-tuc", "glm"),
            ExactSemanticVersion.of(SemanticVersion.of(0, 9, 8, 4))));
    }
}
