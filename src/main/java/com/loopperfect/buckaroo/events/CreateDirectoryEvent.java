package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

import java.nio.file.Path;

public final class CreateDirectoryEvent implements Event {

    public final Path path;

    private CreateDirectoryEvent(final Path path) {
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("path", path)
            .toString();
    }

    public static CreateDirectoryEvent of(final Path path) {
        return new CreateDirectoryEvent(path);
    }
}
