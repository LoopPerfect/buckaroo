package com.loopperfect.buckaroo.virtualterminal.components;

import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class StackLayoutTest {

    @Test
    public void testLayout1() {

        final StackLayout stackLayout = StackLayout.of(
            Text.of("Hello, world. "));

        final Map2D<TerminalPixel> render = stackLayout.render(100);

        assertEquals(100, render.width());
        assertEquals(1, render.height());
    }

    @Test
    public void testLayout2() {

        final StackLayout stackLayout = StackLayout.of(
            Text.of("Hello, world. "),
            Text.of("Testing... testing... "),
            Text.of("123. "));

        final Map2D<TerminalPixel> render = stackLayout.render(100);

        assertEquals(100, render.width());
        assertEquals(3, render.height());
    }
}
