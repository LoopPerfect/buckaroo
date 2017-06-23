package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Event;

import java.net.URL;
import java.nio.file.Path;

public final class GitCheckoutEvent extends Event {

    public final Path target;
    public final String name;

    private GitCheckoutEvent(final Path target, final String name) {
        this.target = target;
        this.name = name;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("date", date)
            .add("threadId", threadId)
            .add("target", target)
            .add("name", name)
            .toString();
    }

    public static GitCheckoutEvent of(final Path target, final String name) {
        return new GitCheckoutEvent(target, name);
    }
}
