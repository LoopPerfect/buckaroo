package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;
import org.fusesource.jansi.Ansi;

public final class ProgressBar implements Component {

    private final float progress;

    private ProgressBar(final float progress) {
        Preconditions.checkArgument(progress >= 0 && progress <= 1);
        this.progress = progress;
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {

        Preconditions.checkArgument(width >= 0);

        final TerminalPixel filled = TerminalPixel.of(
            UnicodeChar.of(' '),
            Color.DEFAULT,
            Color.CYAN);
        final TerminalPixel background = TerminalPixel.of(
            UnicodeChar.of(' '),
            Color.DEFAULT,
            Color.BLUE);
        final int p = (int)Math.ceil(width * progress);
        return Map2D.of(width, 1, TerminalPixel.class, background)
            .modify((x, y, v) -> x <= p ? filled : v);
    }

    public static ProgressBar of(final float progress) {
        return new ProgressBar(progress);
    }
}
