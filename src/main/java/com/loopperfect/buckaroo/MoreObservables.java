package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.javatuples.Pair;

import java.util.*;

public final class MoreObservables {

    private MoreObservables() {

    }

    @Deprecated
    public static <T> Observable<T> skipErrors(final Observable<Single<T>> xs) {
        Preconditions.checkNotNull(xs);
        return xs.flatMap(single -> single.map(Optional::of)
            .onErrorReturn(error -> Optional.empty())
            .flatMapObservable(optional ->
                optional.map(Observable::just).orElseGet(Observable::empty)));
    }

    @Deprecated
    public static <T> Maybe<T> findMax(final Observable<T> xs, final Comparator<T> comparator) {
        Preconditions.checkNotNull(xs);
        Preconditions.checkNotNull(comparator);
        return xs.reduce((x, y) -> (comparator.compare(x, y) < 0) ? y : x);
    }

    public static <T, S> Observable<Map<T, S>> zipMaps(final Map<T, Observable<S>> tasks) {

        Objects.requireNonNull(tasks, "tasks is null");

        return Observable.combineLatest(
            tasks.entrySet()
                .stream()
                .map(entry -> Observable.combineLatest(
                    Observable.just(entry.getKey()),
                    entry.getValue(),
                    Pair::with))
                .collect(ImmutableList.toImmutableList()),
            xs -> Arrays.stream(xs)
                .map(x -> (Pair<T, S>)x)
                .collect(ImmutableMap.toImmutableMap(Pair::getValue0, Pair::getValue1)));
    }

    public static <T, S> Observable<ImmutableMap<T, S>> mergeMaps(final Map<T, Observable<S>> tasks) {

        Objects.requireNonNull(tasks, "tasks is null");

        final ImmutableMap<T,S> initialValue = ImmutableMap.of();
        return Observable.merge(
            tasks.entrySet()
                .stream()
                .map(entry -> entry.getValue()
                    .map(x -> ImmutableMap.of(entry.getKey(), x)))
                .collect(ImmutableList.toImmutableList())
        ).scan(initialValue, MoreMaps::merge);
    }
}
