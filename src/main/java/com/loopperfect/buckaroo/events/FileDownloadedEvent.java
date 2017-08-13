package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

public final class FileDownloadedEvent extends Event {

    public final URI source;
    public final Path destination;

    private FileDownloadedEvent(final URI source, final Path destination) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(destination);

        this.source = source;
        this.destination = destination;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination) * super.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("date", date)
            .add("threadId", threadId)
            .add("source", source)
            .add("destination", destination)
            .toString();
    }

    public static FileDownloadedEvent of(final URI source, final Path destination) {
        return new FileDownloadedEvent(source, destination);
    }
}
