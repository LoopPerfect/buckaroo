package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

import java.nio.file.Path;

public final class FileUnzipEvent implements Event {

    public final Path source;
    public final Path target;

    private FileUnzipEvent(final Path source, final Path target) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(target);

        this.source = source;
        this.target = target;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("source", source)
            .add("target", target)
            .toString();
    }

    public static FileUnzipEvent of(final Path source, final Path target) {
        return new FileUnzipEvent(source, target);
    }
}
