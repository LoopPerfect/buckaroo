package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public final class UrlResource implements Resource {

    public final URL url;

    private UrlResource(final URL url) {
        Preconditions.checkNotNull(url);
        this.url = url;
    }

    @Override
    public String description() {
        return url.toExternalForm();
    }

    @Override
    public IO<Either<IOException, String>> fetch() {
        return IO.of(context -> {
            Preconditions.checkNotNull(context);
            // TODO: Check hash of downloaded content
            return context.http().download(url);
        });
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof UrlResource)) {
            return false;
        }
        final UrlResource other = (UrlResource) obj;
        return Objects.equals(url, other.url);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("url", url)
                .toString();
    }

    public static Resource of(final URL url) {
        return new UrlResource(url);
    }
}
