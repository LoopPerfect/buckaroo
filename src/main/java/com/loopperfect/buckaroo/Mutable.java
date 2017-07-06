package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;

/**
 * A mutable container for any type.
 *
 * Contains a single value of that type that can be modified directly.
 * This container has reference-semantics.
 *
 * This is particularly useful when mutating state from a lambda.
 *
 * @param <T> The type of the value being contained
 */
public final class Mutable<T> {

    public volatile T value;

    public Mutable(final T initialValue) {
        this.value = initialValue;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .addValue(value)
            .toString();
    }
}
