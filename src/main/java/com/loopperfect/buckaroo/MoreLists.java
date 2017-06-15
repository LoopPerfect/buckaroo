package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Created by gaetano on 15/06/17.
 */
public final class MoreLists {
    private MoreLists(){}
    public static <T> ImmutableList<T> concat(ImmutableList<T> a, ImmutableList<T> b) {
        return new ImmutableList.Builder<T>()
            .addAll(a)
            .addAll(b)
            .build();
    }
}
