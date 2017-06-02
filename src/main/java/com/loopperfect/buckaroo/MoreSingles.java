package com.loopperfect.buckaroo;

import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.Objects;
import java.util.function.Function;

public final class MoreSingles {

    private MoreSingles() {

    }

    public static <A extends C, B extends C, C> Observable<C> chain(final Single<A> a, final Function<A, Single<B>> f) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        return Observable.concat(
            a.toObservable().map(x -> (C) x),
            a.flatMap(f::apply).toObservable().map(x -> (C) x));
    }

    public static <T extends U, U> Observable<U> chainObservable(final Single<T> a, final Function<T, Observable<U>> f) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        return Observable.concat(
            a.toObservable(),
            a.flatMapObservable(f::apply));
    }
}
