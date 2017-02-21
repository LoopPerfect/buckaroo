package com.loopperfect.buckaroo.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * Created by gaetano on 21/02/17.
 */
public interface SystemContext {
    default void println() {
        System.out.println();
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
}
