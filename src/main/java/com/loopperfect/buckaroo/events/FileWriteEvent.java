package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

import java.util.Objects;
import java.util.Optional;
import java.nio.file.Path;

public final class FileWriteEvent implements Event {

    public final Path path;
    public final Optional<String> content;

    private FileWriteEvent(final Path path, final Optional<String> content) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(content);
        this.path = path;
        this.content = content;
    }

    public boolean equals(final FileWriteEvent other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(path, other.path) &&
            Objects.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, content);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null &&
                obj instanceof FileWriteEvent &&
                equals((FileWriteEvent) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("path", path)
            .add("content", content)
            .toString();
    }

    public static FileWriteEvent of(final Path path, final Optional<String> content) {
        return new FileWriteEvent(path, content);
    }
}
