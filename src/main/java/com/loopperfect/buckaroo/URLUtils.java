package com.loopperfect.buckaroo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public final class URLUtils {

    private URLUtils() {

    }

    public static Optional<String> getExtension(final URL url) {

        Objects.requireNonNull(url, "url is null");

        final String file = url.getFile();

        if (file.contains(".")) {

            final String sub = file.substring(file.lastIndexOf('.') + 1);

            if (sub.length() == 0) {
                return Optional.empty();
            }

            if (sub.contains("?")) {
                return Optional.of(sub.substring(0, sub.indexOf('?')));
            }

            return Optional.of(sub);
        }

        return Optional.empty();
    }

    public static Optional<String> getExtension(final URI uri) {
        Objects.requireNonNull(uri, "uri is null");
        try {
            return getExtension(uri.toURL());
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }
}
