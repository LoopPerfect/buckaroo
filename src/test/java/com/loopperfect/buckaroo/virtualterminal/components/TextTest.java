package com.loopperfect.buckaroo.virtualterminal.components;

import com.loopperfect.buckaroo.virtualterminal.*;
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

    @Test
    public void testRender1() {

        final Text text = Text.of("abc");

        final Map2D<TerminalPixel> expected = new Map2DBuilder(
            3, 1, TerminalPixel.class, TerminalPixel.of(UnicodeChar.of(' ')))
            .set(0, 0, TerminalPixel.of(UnicodeChar.of('a')))
            .set(1, 0, TerminalPixel.of(UnicodeChar.of('b')))
            .set(2, 0, TerminalPixel.of(UnicodeChar.of('c')))
            .build();

        assertEquals(expected, text.render(3));
    }

    @Test
    public void testRender2() {

        final Text text = Text.of("abc");

        final Map2D<TerminalPixel> expected = new Map2DBuilder(
            3, 1, TerminalPixel.class, TerminalPixel.of(UnicodeChar.of(' ')))
            .set(0, 0, TerminalPixel.of(UnicodeChar.of('a')))
            .set(1, 0, TerminalPixel.of(UnicodeChar.of('b')))
            .set(2, 0, TerminalPixel.of(UnicodeChar.of('c')))
            .build();

        assertEquals(expected, text.render(100));
    }

    @Test
    public void testRender3() {

        final Text text = Text.of("This \nis \na \nmulti-line \ntest");

        final Map2D<TerminalPixel> render = text.render(100);

        assertEquals(29, render.width());
        assertEquals(5, render.height());
    }
}
