package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.toImmutableList;

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

    @SuppressWarnings("unchecked")
    public static <T> Single<List<T>> parallel(final Iterable<Single<T>> xs) {
        Preconditions.checkNotNull(xs);
        return Single.zip(
            xs,
            objects -> Arrays.stream(objects)
                .map(x -> (T) x)
                .collect(toImmutableList())
        );
    }
}
