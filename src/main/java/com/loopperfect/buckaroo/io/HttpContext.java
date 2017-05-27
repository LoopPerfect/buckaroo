package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.loopperfect.buckaroo.Either;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public interface HttpContext {

    Either<IOException, String> download(final URL url);

    Optional<IOException> download(final URL source, final Path target);

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

            @Override
            public Optional<IOException> download(final URL source, final Path target) {
                Preconditions.checkNotNull(source);
                Preconditions.checkNotNull(target);
                // TODO: This should take a file-system as a parameter
                try {
                    final File parentFile = target.getParent().toFile();
                    if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    final File targetFile = target.toFile();
                    Resources.asByteSource(source).copyTo(Files.asByteSink(targetFile));
                    return Optional.empty();
                } catch (final IOException e) {
                    return Optional.of(e);
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

            @Override
            public Optional<IOException> download(final URL source, final Path target) {
                Preconditions.checkNotNull(source);
                Preconditions.checkNotNull(target);
                return Optional.of(new IOException("Fake HTTP context. "));
            }
        };
    }
}
