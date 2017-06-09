package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.loopperfect.buckaroo.Event;

import java.nio.file.Path;

public final class FileHashEvent implements Event {

    public final Path file;
    public final HashCode sha256;

    private FileHashEvent(final Path file, final HashCode sha256) {

        super();

        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(sha256);

        this.file = file;
        this.sha256 = sha256;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("file", file)
            .add("sha256", sha256)
            .toString();
    }

    public static FileHashEvent of(final Path file, final HashCode sha256) {
        return new FileHashEvent(file, sha256);
    }
}
