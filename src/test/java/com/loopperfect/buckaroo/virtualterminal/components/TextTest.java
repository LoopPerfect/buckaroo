package com.loopperfect.buckaroo.virtualterminal.components;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class TextTest {

    @Test
    public void testComputeLines() {

        assertEquals(1, Text.computeLines(5, "abc"));
        assertEquals(2, Text.computeLines(5, "abc\ndef"));
        assertEquals(3, Text.computeLines(1, "abc"));
        assertEquals(3, Text.computeLines(3, "abcdefg"));
        assertEquals(2, Text.computeLines(20, "Here are some nifty progress bars... "));

        assertEquals(1, Text.computeLinesWithWordBreaks(5, "abc"));
        assertEquals(2, Text.computeLinesWithWordBreaks(5, "abc\ndef"));
    }
}