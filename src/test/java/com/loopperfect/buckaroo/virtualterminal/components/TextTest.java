package com.loopperfect.buckaroo.virtualterminal.components;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class TextTest {

    @Test
    public void testIdealSize() {

//        final Text text = Text.of("abc");
//
//        assertEquals(Size.of(3, 1), text.idealSize(Dimensions.of()));
//        assertEquals(Size.of(3, 1), text.idealSize(Dimensions.of(100)));
//        assertEquals(Size.of(3, 1), text.idealSize(Dimensions.of(100, 10)));
//        assertEquals(Size.of(1, 3), text.idealSize(Dimensions.of(1, 10)));
    }

    @Test
    public void testRender() {

//        final Text text = Text.of("abc");
//
//        final Map2D<TerminalPixel> expected = Map2D.of(5, 1, TerminalPixel.class, TerminalPixel.of(UnicodeChar.of(' ')))
//            .set(0, 0, TerminalPixel.of(UnicodeChar.of('a')))
//            .set(1, 0, TerminalPixel.of(UnicodeChar.of('b')))
//            .set(2, 0, TerminalPixel.of(UnicodeChar.of('c')));
//
//        assertEquals(expected, text.render(Size.of(5, 1)));
    }

    @Test
    public void testRenderWrap() {

//        final Text text = Text.of("abc");
//
//        final Map2D<TerminalPixel> expected = Map2D.of(1, 5, TerminalPixel.class, TerminalPixel.of(UnicodeChar.of(' ')))
//            .set(0, 0, TerminalPixel.of(UnicodeChar.of('a')))
//            .set(0, 1, TerminalPixel.of(UnicodeChar.of('b')))
//            .set(0, 2, TerminalPixel.of(UnicodeChar.of('c')));
//
//        assertEquals(expected, text.render(Size.of(1, 5)));
    }

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