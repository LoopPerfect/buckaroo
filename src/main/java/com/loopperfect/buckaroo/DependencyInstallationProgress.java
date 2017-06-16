package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.tasks.DownloadProgress;

public final class DependencyInstallationProgress extends Event {

    public final ImmutableMap<DependencyLock, Event> progress;

    private DependencyInstallationProgress(final ImmutableMap<DependencyLock, Event> progress) {
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

    public static DependencyInstallationProgress of(final ImmutableMap<DependencyLock, Event> progress) {
        return new DependencyInstallationProgress(progress);
    }
}
