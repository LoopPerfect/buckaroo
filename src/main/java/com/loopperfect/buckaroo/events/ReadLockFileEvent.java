package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.DependencyLocks;
import com.loopperfect.buckaroo.Event;

public final class ReadLockFileEvent extends Event {

    public final DependencyLocks locks;

    private ReadLockFileEvent(final DependencyLocks locks) {
        Preconditions.checkNotNull(locks);
        this.locks = locks;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("locks", locks)
            .toString();
    }

    public static ReadLockFileEvent of(final DependencyLocks locks) {
        return new ReadLockFileEvent(locks);
    }
}
