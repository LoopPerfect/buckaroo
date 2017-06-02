package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public final class ReadLockFileEvent implements Event {

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
