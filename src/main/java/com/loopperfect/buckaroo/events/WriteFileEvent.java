package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

import java.util.Objects;
import java.nio.file.Path;

public final class WriteFileEvent extends Event {

    public final Path path;

    private WriteFileEvent(final Path path) {
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    public boolean equals(final WriteFileEvent other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(path, other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null &&
                obj instanceof WriteFileEvent &&
                equals((WriteFileEvent) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("path", path)
            .toString();
    }

    public static WriteFileEvent of(final Path path) {
        return new WriteFileEvent(path);
    }
}