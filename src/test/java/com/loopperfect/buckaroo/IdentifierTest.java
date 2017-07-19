package com.loopperfect.buckaroo;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public final class IdentifierTest {

    @Test
    public void isValid() throws Exception {

        assertTrue(Identifier.isValid("abc"));
        assertTrue(Identifier.isValid("xz"));
        assertTrue(Identifier.isValid("abc_-123"));
        assertTrue(Identifier.isValid("abc-123"));
        assertTrue(Identifier.isValid("abc--123++++"));
        assertTrue(Identifier.isValid("0123"));
        assertTrue(Identifier.isValid("9abc"));

        assertFalse(Identifier.isValid("  ab  c-1 23_"));
        assertFalse(Identifier.isValid("    "));
        assertFalse(Identifier.isValid(""));
        assertFalse(Identifier.isValid("a"));
        assertFalse(Identifier.isValid("abcd/efg"));
        assertFalse(Identifier.isValid("abcd.efg"));
        assertFalse(Identifier.isValid("\n"));
        assertFalse(Identifier.isValid("_abc"));
        assertFalse(Identifier.isValid("++abc"));
        assertFalse(Identifier.isValid("thisidentifieriswaywaywaywaywaywaywaywaywaytoolong"));
    }
}
