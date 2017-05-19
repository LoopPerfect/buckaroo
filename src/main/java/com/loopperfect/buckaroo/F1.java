package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.function.Function;

@FunctionalInterface
public interface F1<A, B> extends Function<A, B> {

    B apply(final A a);

    default <C> F1<A, C> then(final F1<B, C> f) {
        Preconditions.checkNotNull(f);
        return x -> f.apply(apply(x));
    }

    static <A, B> F1<A, B>of(final F1<A, B> f) {
        Preconditions.checkNotNull(f);
        return f;
    }
}
