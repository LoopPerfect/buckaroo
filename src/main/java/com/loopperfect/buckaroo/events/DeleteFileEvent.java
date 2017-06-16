package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Event;

import java.nio.file.Path;

public final class DeleteFileEvent extends Event {

    public final Path path;

    private DeleteFileEvent(final Path path) {
        this.path = path;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("path", path)
            .toString();
    }

    public static DeleteFileEvent of(final Path path) {
        return new DeleteFileEvent(path);
    }
}
