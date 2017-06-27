package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.virtualterminal.*;

public final class ProgressBar implements Component {

    private final float progress;
    private final TerminalPixel fill;
    private final TerminalPixel background;

    private ProgressBar(final float progress, final TerminalPixel fill, final TerminalPixel background) {
        Preconditions.checkArgument(progress >= 0 && progress <= 1);
        this.progress = progress;
        this.fill = fill;
        this.background = background;
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {
        Preconditions.checkArgument(width >= 0);
        final int p = (int)Math.ceil(width * progress);
        return Map2D.of(width, 1, TerminalPixel.class, background)
            .modify((x, y, v) -> x <= p ? fill : v);
    }

    private static final TerminalPixel defaultFill = TerminalPixel.of(
        UnicodeChar.of('\u25A0'),
        Color.WHITE,
        Color.BLACK);

    private static final TerminalPixel defaultBackground = TerminalPixel.of(
        UnicodeChar.of(' '),
        Color.WHITE,
        Color.BLACK);

    public static ProgressBar of(final float progress) {
        return new ProgressBar(progress, defaultFill, defaultBackground);
    }

    public static ProgressBar of(final float progress, final TerminalPixel fill, final TerminalPixel background) {
        return new ProgressBar(progress, fill, background);
    }
}
