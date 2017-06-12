package com.loopperfect.buckaroo.virtualterminal;

import org.fusesource.jansi.Ansi;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class TerminalPixelTest {

    @Test
    public void testEquals() {

        final TerminalPixel a = TerminalPixel.of(
            UnicodeChar.of('h'),
            Color.CYAN,
            Color.MAGENTA);

        final TerminalPixel b = TerminalPixel.of(
            UnicodeChar.of('h'),
            Color.CYAN,
            Color.MAGENTA);

        final TerminalPixel c = TerminalPixel.of(
            UnicodeChar.of('e'),
            Color.RED,
            Color.MAGENTA);

        final TerminalPixel d = TerminalPixel.of(
            UnicodeChar.of('h'),
            Color.CYAN,
            Color.BLACK);

        final TerminalPixel e = TerminalPixel.of(
            UnicodeChar.of('l'),
            Color.GREEN,
            Color.BLUE);

        assertEquals(a, a);
        assertEquals(b, a);

        assertNotEquals(a, c);
        assertNotEquals(a, d);
        assertNotEquals(a, e);
    }

}