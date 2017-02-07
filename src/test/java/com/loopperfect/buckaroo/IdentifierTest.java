package com.loopperfect.buckaroo;

import org.junit.Test;

import static org.junit.Assert.*;

public final class IdentifierTest {

    @Test
    public void isValid() throws Exception {

        assertTrue(Identifier.isValid("abc"));
        assertTrue(Identifier.isValid("abc123"));
        assertTrue(Identifier.isValid("abc-123_"));

        assertFalse(Identifier.isValid("  ab  c-1 23_"));
        assertFalse(Identifier.isValid("    "));
        assertFalse(Identifier.isValid(""));
        assertFalse(Identifier.isValid("\n"));
        assertFalse(Identifier.isValid("thisidentifieriswaywaywaywaywaywaywaywaywaytoolong"));
    }
}
