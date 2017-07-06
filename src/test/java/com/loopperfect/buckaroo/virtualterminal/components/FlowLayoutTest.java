package com.loopperfect.buckaroo.virtualterminal.components;

import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import org.junit.Test;

import static org.junit.Assert.*;

public final class FlowLayoutTest {

    @Test
    public void render1() throws Exception {

        final FlowLayout flowLayout = FlowLayout.of(Text.of("abc"));

        final Map2D<TerminalPixel> render = flowLayout.render(10);

        assertEquals(3, render.width());
        assertEquals(1, render.height());

        assertEquals('a', render.get(0, 0).character.characterCode);
        assertEquals('b', render.get(1, 0).character.characterCode);
        assertEquals('c', render.get(2, 0).character.characterCode);
    }

    @Test
    public void render2() throws Exception {

        final FlowLayout flowLayout = FlowLayout.of(Text.of("abcdefg"), Text.of("12345"));

        final Map2D<TerminalPixel> render = flowLayout.render(10);

        assertEquals(7, render.width());
        assertEquals(2, render.height());
    }
}