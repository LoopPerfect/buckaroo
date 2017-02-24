package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.util.Objects;

public final class FileResource implements Resource {

    public final String path;

    private FileResource(final String path) {
        this.path = Preconditions.checkNotNull(path);
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
        return Objects.equals(path, other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", path).toString();
    }

    public static Resource of(final String path) {
        return new FileResource(path);
    }
}
