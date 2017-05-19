package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

@FunctionalInterface
public interface F4<A, B, C, D, E> {

    E apply(final A a, final B b, final C c, final D d);

    default <F> F4<A, B, C, D, F> then(final F1<E, F> f) {
        Preconditions.checkNotNull(f);
        return (x, y, z, w) -> f.apply(apply(x, y, z, w));
    }

    static <A, B, C, D, E> F4<A, B, C, D, E>of(final F4<A, B, C, D, E> f) {
        Preconditions.checkNotNull(f);
        return f;
    }
}
