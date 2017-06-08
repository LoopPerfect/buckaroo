package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.Map2DBuilder;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;
import org.fusesource.jansi.Ansi;

import java.util.Arrays;

public final class Text implements Component {

    public final String text;

    private Text(final String text) {
        this.text = Preconditions.checkNotNull(text);
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {
        Preconditions.checkArgument(width >= 0);

        final int height = computeLines(width, text);

        final TerminalPixel background = TerminalPixel.of(UnicodeChar.of(' '), Ansi.Color.DEFAULT, Ansi.Color.DEFAULT);
        final Map2DBuilder<TerminalPixel> builder = new Map2DBuilder<>(
            width, height, TerminalPixel.class, background);

        int x = 0;
        int y = 0;

        for (final char i : text.toCharArray()) {
            builder.set(x, y, TerminalPixel.of(UnicodeChar.of(i)));
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
