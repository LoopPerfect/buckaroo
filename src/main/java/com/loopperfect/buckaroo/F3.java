package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

@FunctionalInterface
public interface F3<A, B, C, D> {

    D apply(final A a, final B b, final C c);

    default <E> F3<A, B, C, E> then(final F1<D, E> f) {
        Preconditions.checkNotNull(f);
        return (x, y, z) -> f.apply(apply(x, y, z));
    }

    static <A, B, C, D> F3<A, B, C, D>of(final F3<A, B, C, D> f) {
        Preconditions.checkNotNull(f);
        return f;
    }
}
