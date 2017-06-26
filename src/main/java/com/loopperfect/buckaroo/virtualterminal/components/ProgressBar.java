package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;

public final class ProgressBar implements Component {

    private final float progress;
    private final char barCharacter;
    private final Color foreground;
    private final Color background;

    private ProgressBar(final float progress, final char barCharacter, final Color foreground, final Color background) {
        Preconditions.checkArgument(progress >= 0 && progress <= 1);
        this.progress = progress;
        this.barCharacter = barCharacter;
        this.foreground = foreground;
        this.background = background;
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {
        Preconditions.checkArgument(width >= 0);
        final TerminalPixel filledPixel = TerminalPixel.of(
            UnicodeChar.of(barCharacter),
            Color.DEFAULT,
            foreground);
        final TerminalPixel backgroundPixel = TerminalPixel.of(
            UnicodeChar.of(' '),
            Color.DEFAULT,
            background);
        final int p = (int)Math.ceil(width * progress);
        return Map2D.of(width, 1, TerminalPixel.class, backgroundPixel)
            .modify((x, y, v) -> x <= p ? filledPixel : v);
    }

    public static ProgressBar of(final float progress) {
        return new ProgressBar(progress, '#', Color.DEFAULT, Color.DEFAULT);
    }
}
