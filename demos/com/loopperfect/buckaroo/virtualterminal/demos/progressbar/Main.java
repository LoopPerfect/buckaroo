package com.loopperfect.buckaroo.virtualterminal.demos.progressbar;

import com.loopperfect.buckaroo.virtualterminal.*;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public final class Main {

    private Main() {
        super();
    }

    public static void main(final String[] args) throws InterruptedException {

        AnsiConsole.systemInstall();

        final TerminalBuffer buffer = new TerminalBuffer();

        int progress = 0;

        buffer.flip(render(100, 1, progress));

        while (progress < 100) {

            progress++;
            buffer.flip(render(100, 1, progress));

            Thread.sleep(10);
        }
    }

    private static Map2D<TerminalPixel> render(final int width, final int height, final int progress) {
        return Map2D.of(
            width, height, TerminalPixel.class, TerminalPixel.of(UnicodeChar.of(' ')))
            .modify((x, y, v) -> y == 0 ? (x <= progress ? v.setBackground(Color.BLACK) : v) : v);
    }
}
