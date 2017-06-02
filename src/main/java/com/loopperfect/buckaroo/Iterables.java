package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

@Deprecated
public final class Iterables {

    private Iterables() {

    }

    public static <T> Iterable<T> of(final ImmutableList<T> xs) {
        Preconditions.checkNotNull(xs);
        return xs::iterator;
    }
}
