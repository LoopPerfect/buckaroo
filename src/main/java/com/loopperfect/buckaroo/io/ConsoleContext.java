package com.loopperfect.buckaroo.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by gaetano on 21/02/17.
 */
public interface ConsoleContext {
    default void println() {
        println("");
    }

    default void println(final String x) {
        System.out.println(x);
    }

    default Optional<String> readln() {
        if (System.console() != null) {
            return Optional.of(System.console().readLine());
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return Optional.of(reader.readLine());
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    static ConsoleContext actual() {
        return new ConsoleContext() {
        };
    }

    static ConsoleContext fake() {
        return create(x -> null, Optional::empty);
    }

    static ConsoleContext create(final Function<String, Void> printer, final Supplier<Optional<String>> reader) {

        return new ConsoleContext() {
            @Override
            public void println(final String x) {
                printer.apply(x);
            }

            @Override
            public Optional<String> readln() {
                return reader.get();
            }
        };
    }
}
