package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

@FunctionalInterface
public interface F2<A, B, C> {

    C apply(final A a, final B b);

    default <D> F2<A, B, D> then(final F1<C, D> f) {
        Preconditions.checkNotNull(f);
        return (x, y) -> f.apply(apply(x, y));
    }

    static <A, B, C> F2<A, B, C>of(final F2<A, B, C> f) {
        Preconditions.checkNotNull(f);
        return f;
    }
}
