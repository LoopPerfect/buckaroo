package com.loopperfect.buckaroo.virtualterminal.demos.flash;

import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalBuffer;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public final class Main {

    public static void main(final String[] args) throws InterruptedException {

        AnsiConsole.systemInstall();

        final TerminalBuffer buffer  = new TerminalBuffer();

        final Map2D<TerminalPixel> image = Map2D.of(
            20, 20, TerminalPixel.class, TerminalPixel.of(UnicodeChar.of('a')));

        final int wait = 500;

        for (int i = 0; i < 10; i++) {

            buffer.flip(image.modify((x, y, v) -> v.setBackground(Ansi.Color.CYAN)));
            Thread.sleep(wait);

            buffer.flip(image.modify((x, y, v) -> v.setBackground(Ansi.Color.GREEN)));

            Thread.sleep(wait);

            buffer.flip(image.modify((x, y, v) -> v.setBackground(Ansi.Color.MAGENTA)));

            Thread.sleep(wait);

            buffer.flip(image.modify((x, y, v) -> v.setBackground(Ansi.Color.YELLOW)));

            Thread.sleep(wait);
        }
    }
}
