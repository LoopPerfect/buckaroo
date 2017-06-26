package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public final class MoreLists {

    private MoreLists() {}

    public static <T> ImmutableList<T> concat(final ImmutableList<? extends T> a, final ImmutableList<? extends T> b) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);
        return new ImmutableList.Builder<T>()
            .addAll(a)
            .addAll(b)
            .build();
    }

    public static <T> ImmutableList<T> append(final ImmutableList<T> a, final T b) {
        Preconditions.checkNotNull(a);
        return new ImmutableList.Builder<T>()
            .addAll(a)
            .add(b)
            .build();
    }
}
