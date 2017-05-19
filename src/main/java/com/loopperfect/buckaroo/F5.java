package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

@FunctionalInterface
public interface F5<A, B, C, D, E, F> {

    F apply(final A a, final B b, final C c, final D d, final E e);

    default <G> F5<A, B, C, D, E, G> then(final F1<F, G> f) {
        Preconditions.checkNotNull(f);
        return (x, y, z, w, v) -> f.apply(apply(x, y, z, w, v));
    }

    static <A, B, C, D, E, F> F5<A, B, C, D, E, F>of(final F5<A, B, C, D, E, F> f) {
        Preconditions.checkNotNull(f);
        return f;
    }
}
