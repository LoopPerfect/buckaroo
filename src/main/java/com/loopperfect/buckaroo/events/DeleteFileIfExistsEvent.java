package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

import java.nio.file.Path;

public final class DeleteFileIfExistsEvent extends Event {

    public final Path path;
    public boolean didDelete;

    private DeleteFileIfExistsEvent(final Path path, final boolean didDelete) {
        Preconditions.checkNotNull(path);
        this.path = path;
        this.didDelete = didDelete;
    }

    // TODO: equals, hashCode

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
