package com.loopperfect.buckaroo;

import org.junit.Test;

import static org.junit.Assert.*;

public final class PartialRecipeIdentifierTest {

    @Test
    public void equals() throws Exception {

        assertEquals(
            PartialRecipeIdentifier.of(Identifier.of("github"), Identifier.of("org"),
                Identifier.of("some_lib")),
            PartialRecipeIdentifier.of(Identifier.of("github"), Identifier.of("org"),
                Identifier.of("some_lib"))
        );
    }

    @Test
    public void isSatisfiedBy() throws Exception {

        final RecipeIdentifier identifier = RecipeIdentifier.of(
            Identifier.of("github"),
            Identifier.of("loopperfect"),
            Identifier.of("valuable"));

        assertTrue(PartialRecipeIdentifier.of(Identifier.of("valuable")).isSatisfiedBy(identifier));

        assertTrue(PartialRecipeIdentifier.of(Identifier.of("loopperfect"), Identifier.of("valuable"))
            .isSatisfiedBy(identifier));

        assertTrue(PartialRecipeIdentifier.of(
            Identifier.of("github"), Identifier.of("loopperfect"), Identifier.of("valuable"))
            .isSatisfiedBy(identifier));

        assertFalse(PartialRecipeIdentifier.of(Identifier.of("invaluable")).isSatisfiedBy(identifier));

        assertFalse(PartialRecipeIdentifier.of(Identifier.of("anotherorg"), Identifier.of("valuable"))
            .isSatisfiedBy(identifier));

        assertFalse(PartialRecipeIdentifier.of(Identifier.of("bitbucket"), Identifier.of("loopperfect"),
            Identifier.of("valuable")).isSatisfiedBy(identifier));
    }
}