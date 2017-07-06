package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Project;

public final class ReadProjectFileEvent extends Event {

    private ReadProjectFileEvent() {

    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj != null && obj instanceof ReadProjectFileEvent;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .toString();
    }

    public static ReadProjectFileEvent of() {
        return new ReadProjectFileEvent();
    }
}
