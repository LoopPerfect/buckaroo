package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Function;

public final class Routines {

    private Routines() {

    }

    public static Optional<String> requestString(final String message, final String validationMessage, final Function<String, Boolean> validator) {

        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(validationMessage);
        Preconditions.checkNotNull(validator);

        System.out.println(message);

        final BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            try {
                final String input = buffer.readLine();

                if (validator.apply(input)) {
                    return Optional.of(input);
                }

                System.out.println(validationMessage);

            } catch (final IOException e) {
                return Optional.empty();
            }
        }
    }
}
