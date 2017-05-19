package com.loopperfect.buckaroo.io;

import com.loopperfect.buckaroo.Unit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public interface ConsoleContext {

    default Unit println() {
        println("");
        return Unit.of();
    }

    Unit println(final String x);

    Optional<String> readln();

    static ConsoleContext actual() {
        return new ConsoleContext() {

            @Override
            public Unit println(final String x) {
                System.out.println(x);
                return Unit.of();
            }

            @Override
            public Optional<String> readln() {
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
        };
    }

    static ConsoleContext fake() {

        return new ConsoleContext() {

            @Override
            public Unit println(final String x) {
                return Unit.of();
            }

            @Override
            public Optional<String> readln() {
                return Optional.empty();
            }
        };
    }
}
