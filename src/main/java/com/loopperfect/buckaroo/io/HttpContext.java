package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Either;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public interface HttpContext {

    Either<IOException, String> download(final URL url);

    static HttpContext actual() {

        return new HttpContext() {

            @Override
            public Either<IOException, String> download(final URL url) {
                Preconditions.checkNotNull(url);
                try {
                    final URLConnection connection = url.openConnection();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.UTF_8))) {
                        final String content = reader.lines()
                                .collect(Collectors.joining("\n"));
                        return Either.right(content);
                    }
                } catch (final IOException e) {
                    return Either.left(e);
                }
            }
        };
    }

    static HttpContext fake() {

        return new HttpContext() {

            @Override
            public Either<IOException, String> download(final URL url) {
                Preconditions.checkNotNull(url);
                return Either.left(new IOException("Fake HTTP context. "));
            }
        };
    }
}
