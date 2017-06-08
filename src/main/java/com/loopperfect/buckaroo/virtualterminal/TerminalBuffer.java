package com.loopperfect.buckaroo.virtualterminal;

import com.google.common.base.Preconditions;
import org.fusesource.jansi.Ansi;

public final class TerminalBuffer {

    private Map2D<TerminalPixel> current;

    public TerminalBuffer(final Map2D<TerminalPixel> current) {
        Preconditions.checkNotNull(current);
        this.current = current;
    }

    public TerminalBuffer() {
        current = null;
    }

    public void flip(final Map2D<TerminalPixel> next) {
        Preconditions.checkNotNull(next);
        if (current != null) {
            clear(current);
        }
        render(next);
        current = next;
    }

    private static void clear(final Map2D<TerminalPixel> image) {
        Preconditions.checkNotNull(image);
        System.out.print(Ansi.ansi().cursorUpLine(image.height()));
        for (int y = 0; y < image.height(); y++) {
            System.out.print(Ansi.ansi().eraseLine(Ansi.Erase.BACKWARD));
        }
    }

    private static void render(final Map2D<TerminalPixel> image) {
        Preconditions.checkNotNull(image);
        // TODO: Use a diffing algorithm to minimize re-writes
        for (int y = 0; y < image.height(); y++) {
            for (int x = 0; x < image.width(); x++) {
                final TerminalPixel pixel = image.get(x, y);
                System.out.print(Ansi.ansi().bg(pixel.background).fg(pixel.foreground).a((char) pixel.character.characterCode));
            }
            System.out.print(Ansi.ansi().reset().newline());
        }
        System.out.print(Ansi.ansi().reset());
    }
}
