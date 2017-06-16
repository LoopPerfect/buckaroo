package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;

import java.time.Instant;
import java.util.Date;

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

    public String toDebugString() {
        return MoreObjects
            .toStringHelper(this)
            .add("type", "Event")
            .add("date", date)
            .add("threadId", threadId)
            .toString();
    }

    // TODO: Implement the visitor pattern?

}
