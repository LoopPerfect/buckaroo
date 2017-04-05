package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class FileResource implements Resource {

    public final String path;
    public final Optional<String> sha256;

    private FileResource(final String path, final Optional<String> sha256) {
        this.path = Preconditions.checkNotNull(path);
        this.sha256 = Preconditions.checkNotNull(sha256);
    }

    @Override
    public String description() {
        return path;
    }

    @Override
    public IO<Either<IOException, String>> fetch() {
        return IO.readFile(path);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof FileResource)) {
            return false;
        }
        final FileResource other = (FileResource) obj;
        return Objects.equals(path, other.path) &&
            Objects.equals(sha256, other.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("path", path)
            .add("sha256", sha256)
            .toString();
    }

    public static Resource of(final String path) {
        return new FileResource(path, Optional.empty());
    }

    public static Resource of(final String path, String sha256) {
        return new FileResource(path, Optional.of(sha256));
    }
}
