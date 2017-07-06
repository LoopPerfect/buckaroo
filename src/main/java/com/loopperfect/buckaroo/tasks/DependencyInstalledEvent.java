package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.DependencyLock;
import com.loopperfect.buckaroo.Event;

public final class DependencyInstalledEvent extends Event {

    public final DependencyLock dependency;

    private DependencyInstalledEvent(final DependencyLock dependency) {
        super();
        Preconditions.checkNotNull(dependency);
        this.dependency = dependency;
    }

    // TODO: equals, hashCode

    public static DependencyInstalledEvent of(final DependencyLock dependency) {
        return new DependencyInstalledEvent(dependency);
    }
}
