package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

import java.nio.file.Path;
import java.util.Objects;

public final class DeleteFileIfExistsEvent extends Event {

    public final Path path;
    public final boolean didDelete;

    private DeleteFileIfExistsEvent(final Path path, final boolean didDelete) {
        Preconditions.checkNotNull(path);
        this.path = path;
        this.didDelete = didDelete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, didDelete) * super.hashCode();
    }

    public boolean equals(final DeleteFileIfExistsEvent other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(path, other.path) &&
            Objects.equals(didDelete, other.didDelete);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj != null &&
            obj instanceof DeleteFileIfExistsEvent &&
            equals((DeleteFileIfExistsEvent) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("path", path)
            .add("didDelete", didDelete)
            .toString();
    }

    public static DeleteFileIfExistsEvent of(final Path path) {
        return new DeleteFileIfExistsEvent(path, true);
    }

    public static DeleteFileIfExistsEvent of(final Path path, final boolean didDelete) {
        return new DeleteFileIfExistsEvent(path, didDelete);
    }
}
