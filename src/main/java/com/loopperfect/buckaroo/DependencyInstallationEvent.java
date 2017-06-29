package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.javatuples.Pair;

public final class DependencyInstallationEvent extends Event {

    public final Pair<DependencyLock, Event> progress;

    private DependencyInstallationEvent(final Pair<DependencyLock, Event> progress) {
        super();
        Preconditions.checkNotNull(progress);
        this.progress = progress;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("progress", progress)
            .toString();
    }

    public static DependencyInstallationEvent of(final Pair<DependencyLock, Event> progress) {
        return new DependencyInstallationEvent(progress);
    }
}
