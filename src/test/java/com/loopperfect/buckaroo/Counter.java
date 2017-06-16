package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;

public final class Counter {

    public int count;

    public Counter() {
        count = 0;
    }

    public void increment() {
        count++;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .addValue(count)
            .toString();
    }
}
