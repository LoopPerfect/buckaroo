package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DependencyTest {

    @Test
    public void testIsSatisfiedBy() throws Exception {

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

        assertTrue(Dependency.of(Identifier.of("magic-lib"), ExactSemanticVersion.of(SemanticVersion.of(1))).isSatisfiedBy(recipe));
        assertTrue(Dependency.of(Identifier.of("magic-lib"), AnySemanticVersion.of()).isSatisfiedBy(recipe));
        assertTrue(Dependency.of(Identifier.of("magic-lib"), BoundedSemanticVersion.atMost(SemanticVersion.of(2))).isSatisfiedBy(recipe));
        assertTrue(Dependency.of(Identifier.of("magic-lib"), BoundedSemanticVersion.atLeast(SemanticVersion.of(1, 1))).isSatisfiedBy(recipe));

        assertFalse(Dependency.of(Identifier.of("magic-lib"), ExactSemanticVersion.of(SemanticVersion.of(2))).isSatisfiedBy(recipe));
        assertFalse(Dependency.of(Identifier.of("awesome-lib"), AnySemanticVersion.of()).isSatisfiedBy(recipe));
        assertFalse(Dependency.of(Identifier.of("magic-lib"), BoundedSemanticVersion.atLeast(SemanticVersion.of(7))).isSatisfiedBy(recipe));
    }
}