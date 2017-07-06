package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

import java.nio.file.Path;

public final class FileCopyEvent extends Event {

    public final Path source;
    public final Path destination;

    private FileCopyEvent(final Path source, final Path destination) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(destination);

        this.source = source;
        this.destination = destination;
    }

    // TODO: equals, hashCode


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("source", source)
            .add("destination", destination)
            .toString();
    }

    public static FileCopyEvent of(final Path source, final Path destination) {
        return new FileCopyEvent(source, destination);
    }
}
