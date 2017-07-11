package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.sources.RecipeFetchException;
import com.loopperfect.buckaroo.views.GenericEventRenderer;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.TerminalBuffer;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.ListLayout;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;

import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.time.Instant;
import java.util.Arrays;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.loopperfect.buckaroo.Main.TERMINAL_WIDTH;

public final class ErrorHandler {

    public static void handleErrors(final Throwable error, final TerminalBuffer buffer, final FileSystem fs) {

        Preconditions.checkNotNull(error);
        Preconditions.checkNotNull(buffer);
        Preconditions.checkNotNull(fs);

        if (error instanceof RenderableException) {
            buffer.flip(((RenderableException) error).render().render(Main.TERMINAL_WIDTH));
            return;
        }

        if (error instanceof SocketTimeoutException) {
            final Component component = StackLayout.of(
                Text.of("Error! \n" + error.toString(), Color.RED),
                Text.of("Server did not respond in time. Are you connected to the internet?"));
            buffer.flip(component.render(Main.TERMINAL_WIDTH));
            return;
        }

        handleUnexpectedError(error, buffer, fs);
    }

    private static void handleUnexpectedError(final Throwable error, final TerminalBuffer buffer, final FileSystem fs) {

        Preconditions.checkNotNull(error);
        Preconditions.checkNotNull(buffer);
        Preconditions.checkNotNull(fs);

        buffer.flip(
            StackLayout.of(
                Text.of("Error! \n" + error.toString(), Color.RED),
                Text.of("Get help at https://github.com/loopperfect/buckaroo/issues", Color.BLUE),
                Text.of("The stacktrace was written to buckaroo-stacktrace.log. ", Color.YELLOW))
                .render(TERMINAL_WIDTH));
        try {
            final String trace = Arrays
                .stream(error.getStackTrace())
                .map(StackTraceElement::toString)
                .reduce(Instant.now().toString() + ": ", (a, b) -> a + "\n" + b);

            EvenMoreFiles.writeFile(
                fs.getPath("buckaroo-stacktrace.log"),
                trace,
                Charset.defaultCharset(),
                true);
        } catch (final Throwable ignored) {

        }
    }
}
