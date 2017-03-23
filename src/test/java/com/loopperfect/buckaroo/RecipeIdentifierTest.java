package com.loopperfect.buckaroo;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public final class RecipeIdentifierTest {

    @Test
    public void isValid() throws Exception {

        assertEquals(
                RecipeIdentifier.of(Identifier.of("boost"), Identifier.of("any")),
                RecipeIdentifier.of(Identifier.of("boost"), Identifier.of("any")));

        assertNotEquals(
                RecipeIdentifier.of(Identifier.of("boost"), Identifier.of("any")),
                RecipeIdentifier.of(Identifier.of("boost"), Identifier.of("variant")));

        assertNotEquals(
                RecipeIdentifier.of(Identifier.of("boost"), Identifier.of("any")),
                RecipeIdentifier.of(Identifier.of("custom"), Identifier.of("any")));

        assertEquals(
                RecipeIdentifier.parse("     boost/any  "),
                Optional.of(RecipeIdentifier.of(Identifier.of("boost"), Identifier.of("any"))));

        assertEquals(RecipeIdentifier.parse(""), Optional.empty());
        assertEquals(RecipeIdentifier.parse("/"), Optional.empty());
        assertEquals(RecipeIdentifier.parse("ab/"), Optional.empty());
        assertEquals(RecipeIdentifier.parse("  /defg"), Optional.empty());
        assertEquals(RecipeIdentifier.parse(" abc /defg"), Optional.empty());
    }
}
