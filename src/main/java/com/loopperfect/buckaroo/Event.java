package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Marker interface for objects that represent events that may
 * occur when Buckaroo is running a command.
 *
 * Examples of events might be:
 *  - a file was downloaded
 *  - a dependency was resolved
 *  - a Git clone finished
 */
public abstract class Event {

    public final Date date = Date.from(Instant.now());
    public final long threadId = Thread.currentThread().getId();

    private boolean equals(final Event other) {
        return Objects.equals(date, other.date) &&
            (threadId == other.threadId);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof Event && equals((Event)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, threadId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("date", date)
            .add("threadId", threadId)
            .toString();
    }
}
