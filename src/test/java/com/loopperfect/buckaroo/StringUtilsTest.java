package com.loopperfect.buckaroo;

import org.junit.Test;

import static org.junit.Assert.*;

public final class StringUtilsTest {

    @Test
    public void escapeStringGitHubStyle() {

        assertEquals("abc", StringUtils.escapeStringGitHubStyle("abc"));
        assertEquals("a--bc-d", StringUtils.escapeStringGitHubStyle("a--bc%%#*d"));
    }

    @Test
    public void escapeStringAsFilename() throws Exception {

        // %2Fa%2F%5C%2B%40az
        final String escaped = StringUtils.escapeStringAsFilename("/a/\\+@az-");

        assertTrue(escaped.contains("a"));
        assertTrue(escaped.contains("z"));
        assertTrue(escaped.contains("-"));

        assertTrue(!escaped.contains("/"));
        assertTrue(!escaped.contains("\\"));
        assertTrue(!escaped.contains("@"));
        assertTrue(!escaped.contains("+"));
    }
}