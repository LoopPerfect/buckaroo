package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public final class ReadConfigFileEvent implements Event {

    public final BuckarooConfig config;

    private ReadConfigFileEvent(final BuckarooConfig config) {
        Preconditions.checkNotNull(config);
        this.config = config;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("config", config)
            .toString();
    }

    public static ReadConfigFileEvent of(final BuckarooConfig config) {
        return new ReadConfigFileEvent(config);
    }
}
