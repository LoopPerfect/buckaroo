package com.loopperfect.buckaroo.virtualterminal;

import com.google.common.base.Preconditions;
import org.fusesource.jansi.Ansi;

import java.util.function.Consumer;

public final class TerminalBuffer {

    private final Object lock = new Object();

    private final Consumer<String> consumer;

    private transient volatile Map2D<TerminalPixel> current;

    public TerminalBuffer() {
        this(System.out::print);
    }

    public TerminalBuffer(final Consumer<String> consumer) {
        this.consumer = consumer;
    }

    public void flip(final Map2D<TerminalPixel> next) {
        Preconditions.checkNotNull(next);
        final Ansi nextAnsi = render(next);
        synchronized (lock) {
            if (current != null) {
                consumer.accept(clear(current).toString());
            }
            consumer.accept(nextAnsi.toString());
            current = next;
        }
    }

    public static Ansi clear(final Map2D<TerminalPixel> image) {
        Preconditions.checkNotNull(image);
        final Ansi ansi = Ansi.ansi();
        for (int y = 0; y < image.height(); y++) {
            ansi.eraseLine(Ansi.Erase.ALL)
                .cursorToColumn(0)
                .cursorUpLine();
        }
        return ansi;
    }

    public static Ansi render(final Map2D<TerminalPixel> image) {
        Preconditions.checkNotNull(image);
        // TODO: Use a diffing algorithm to minimize re-writes
        final Ansi ansi = Ansi.ansi().reset();
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                final TerminalPixel pixel = image.get(x, y);
                final boolean updateStyle = x == 0 ||
                    !image.get(x - 1, y).hasSameStyling(pixel);
                if (updateStyle) {
                    ansi.bg(pixel.background.toAnsi())
                        .fg(pixel.foreground.toAnsi());
                }
                ansi.a((char) pixel.character.characterCode);
            }
            ansi.reset().newline();
        }
        ansi.reset();
        return ansi;
    }
}
