package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.sources.RecipeNotFoundException;
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

/**
 * Created by gaetano on 11/07/17.
 */
public class ErrorHandler {

    private static int TERMINAL_WIDTH = 60;

    public static void handleErrors(final Throwable error, final TerminalBuffer buffer, final FileSystem fs){
        handleRecipeError(error, buffer);
        handleCookbookError(error, buffer);
        handleHashMismatchError(error, buffer);
        handleSocketTimeoutError(error, buffer);
        handleUnexpectedError(error, buffer, fs);
    }

    private static void handleRecipeError(final Throwable error, final TerminalBuffer buffer) {
        if (error instanceof RecipeNotFoundException) {
            final RecipeNotFoundException notFound = (RecipeNotFoundException)error;

            final ImmutableList<Component> candidates =
                Streams.stream(notFound.source.findCandidates(notFound.identifier))
                    .limit(3)
                    .map(GenericEventRenderer::render)
                    .collect(toImmutableList());

            if(candidates.size()>0) {
                buffer.flip(
                    StackLayout.of(
                        Text.of("Error! \n" + error.toString(), Color.RED),
                        Text.of("Maybe you meant to install one of the following?"),
                        ListLayout.of(candidates)).render(TERMINAL_WIDTH));
            } else {
                buffer.flip(Text.of("Error! \n" + error.toString(), Color.RED).render(TERMINAL_WIDTH));
            }
        }
    }

    private static void handleCookbookError(final Throwable error, final TerminalBuffer buffer) {
        if(error instanceof CookbookUpdateException) {
            buffer.flip(Text.of("Error! \n" + error.toString(), Color.RED).render(TERMINAL_WIDTH));
        }
    }

    private static void handleSocketTimeoutError(final Throwable error, final TerminalBuffer buffer) {
        if (error instanceof SocketTimeoutException) {
            buffer.flip(StackLayout.of(
                Text.of("Error! \n" + error.toString(), Color.RED),
                Text.of("Server did not respond in time. Are you connected to the internet?")
            ).render(TERMINAL_WIDTH));
        }
    }


    private static void handleHashMismatchError(final Throwable error, final TerminalBuffer buffer) {
        if(error instanceof HashMismatchException) {
            buffer.flip(StackLayout.of(
                Text.of("Error! \n" + error.toString(), Color.RED),
                Text.of("Reasons for this exceptions are:"),
                ListLayout.of(
                    Text.of("The cookbook listed a wrong hash"),
                    Text.of("The source delivered a wrong file"),
                    Text.of("An error occured while downloading"),
                    Text.of("An error occured while extracting the archive")
                )
            ).render(TERMINAL_WIDTH));
        }
    }

    private static void handleUnexpectedError(final Throwable error, final TerminalBuffer buffer, final FileSystem fs) {
        buffer.flip(
            StackLayout.of(
                Text.of("Error! \n" + error.toString(), Color.RED),
                Text.of("Please checkout https://github.com/loopperfect/buckaroo/issues", Color.BLUE),
                Text.of("The stacktrace was written to buckaroo-stacktrace.log. ", Color.YELLOW)
            ).render(TERMINAL_WIDTH));
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
