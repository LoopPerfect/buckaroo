package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Event;
import java.nio.file.Path;
import java.net.URL;

public final class GitCloneEvent extends Event {

    public final String url;
    public final Path target;

    private GitCloneEvent(final String url, final Path target) {
        this.url = url;
        this.target = target;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("date", date)
            .add("threadId", threadId)
            .add("url", url)
            .add("target", target)
            .toString();
    }

    public static GitCloneEvent of(final String url, final Path target) {
        return new GitCloneEvent(url, target);
    }
}
