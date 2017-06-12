package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.virtualterminal.*;


import java.util.Arrays;

public final class Text implements Component {

    public final String text;
    public final Color foreground;
    public final Color background;

    private Text(final String text) {
        this.text = Preconditions.checkNotNull(text);
        this.foreground = Preconditions.checkNotNull(Color.DEFAULT);
        this.background = Preconditions.checkNotNull(Color.TRANSPARENT);
    }

    private Text(final String text, final Color foreground, final Color background) {
        this.text = Preconditions.checkNotNull(text);
        this.foreground = Preconditions.checkNotNull(foreground);
        this.background = Preconditions.checkNotNull(background);
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {
        Preconditions.checkArgument(width >= 0);

        final int height = computeLines(width, text);

        final TerminalPixel backgroundPixel = TerminalPixel.of(UnicodeChar.of(' '), foreground, background);
        final Map2DBuilder<TerminalPixel> builder = new Map2DBuilder<>(
            Math.min(width, text.length()), height, TerminalPixel.class, backgroundPixel);

        int x = 0;
        int y = 0;

        for (final char i : text.toCharArray()) {
            builder.set(x, y, TerminalPixel.of(UnicodeChar.of(i),foreground, background));
            x++;
            if (x >= builder.width()) {
                x = 0;
                y++;
            }
            if (y >= builder.height()) {
                break;
            }
        }

        return builder.build();
    }

    public static Text of(final String text) {
        return new Text(text);
    }
    public static Text of(final String text, final Color foreground) {
        return new Text(text, foreground, Color.TRANSPARENT);
    }
    public static Text of(final String text, final Color foreground, final Color background) {
        return new Text(text, foreground, background);
    }

    public static int computeLines(final int width, final String text) {
        Preconditions.checkArgument(width > 0);
        Preconditions.checkNotNull(text);
        return Arrays.stream(text.split("\n"))
            .mapToInt(x -> computeHeightIgnoreReturns(width, x))
            .sum();
    }

    private static int computeHeightIgnoreReturns(final int width, final String text) {
        Preconditions.checkArgument(width > 0);
        Preconditions.checkNotNull(text);
        return Math.max(1, text.length() / width + (text.length() % width > 0 ? 1 : 0));
    }

    public static int computeLinesWithWordBreaks(final int width, final String text) {
        Preconditions.checkArgument(width > 0);
        Preconditions.checkNotNull(text);
        return Arrays.stream(text.split("\n"))
            .mapToInt(x -> computeHeightWithWordBreaks(width, x))
            .sum();
    }

    private static int computeHeightWithWordBreaks(final int width, final String text) {
        Preconditions.checkArgument(width > 0);
        Preconditions.checkNotNull(text);
        final String[] words = text.split("\\s+");
        int x = 0;
        int y = 0;
        for (final String word : words) {
            x += word.length();
            if (x + 1 >= width) {
                x = 0;
                y++;
            }
        }
        return Math.max(1, y);
    }
}
