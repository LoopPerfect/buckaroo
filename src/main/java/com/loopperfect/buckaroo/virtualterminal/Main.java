package com.loopperfect.buckaroo.virtualterminal;

import com.loopperfect.buckaroo.virtualterminal.components.*;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class Main {

    public static void main(final String[] args) throws InterruptedException, IOException {

        final Consumer<Integer> callback = x -> {
            System.out.println("GOT " + x);
        };

        final Runnable inputListener = () -> {
            try {
                while (true) {
                    final int c = System.in.read();
                    callback.accept(c);
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        };

        AnsiConsole.systemInstall();

        final ExecutorService executor = Executors.newCachedThreadPool();

        executor.submit(inputListener);

        final TerminalBuffer buffer = new TerminalBuffer();

        final Component app = StackLayout.of(
            TerminalPixel.of(UnicodeChar.of('*'), Ansi.Color.WHITE, Ansi.Color.BLUE),
            Text.of("Hello, how are you today? "),
            Text.of("Here are some nifty progress bars... "),
            ProgressBar.of(0.00f),
            ProgressBar.of(0.33f),
            ProgressBar.of(0.66f),
            ProgressBar.of(1.00f),
            Text.of("And here is a flow layout... "),
            FlowLayout.of(
                Box.of(TerminalPixel.of(UnicodeChar.of('*'), Ansi.Color.MAGENTA, Ansi.Color.GREEN)),
                Box.of(TerminalPixel.of(UnicodeChar.of('*'), Ansi.Color.WHITE, Ansi.Color.BLUE), 3, 7),
                Box.of(TerminalPixel.of(UnicodeChar.of('*'), Ansi.Color.RED, Ansi.Color.CYAN), 5, 5)));

        buffer.flip(app.render(50));


        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
//            for (final String line : bufferedReader.lines().collect(Collectors.toList())) {
//                System.out.println(line);
//            }
            if (bufferedReader.ready()) {
//                System.out.println(bufferedReader.read());
            }
        }

//        executor.shutdown();
    }
}
