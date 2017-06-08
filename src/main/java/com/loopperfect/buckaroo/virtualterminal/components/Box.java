package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;

public final class Box implements Component {

    private final TerminalPixel pixel;
    private final int width;
    private final int height;

    private Box(final TerminalPixel pixel, final int width, final int height) {
        this.pixel = Preconditions.checkNotNull(pixel);
        Preconditions.checkArgument(width > 0);
        Preconditions.checkArgument(height > 0);
        this.width = width;
        this.height = height;
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {
        return Map2D.of(this.width, this.height, TerminalPixel.class, pixel);
    }

    public static Box of(final TerminalPixel pixel, final int width, final int height) {
        return new Box(pixel, width, height);
    }

    public static Box of(final TerminalPixel pixel) {
        return new Box(pixel, 1, 1);
    }
}
