package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Emitter;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.javatuples.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class MoreObservables {

    private MoreObservables() {

    }

    public static <T> Observable<T> skipErrors(final Observable<Single<T>> xs) {
        Preconditions.checkNotNull(xs);
        return xs.flatMap(single -> single.map(Optional::of)
            .onErrorReturn(error -> Optional.empty())
            .flatMapObservable(optional ->
                optional.map(Observable::just).orElseGet(Observable::empty)));
    }

    public static <T> Maybe<T> findMax(final Observable<T> xs, final Comparator<T> comparator) {
        Preconditions.checkNotNull(xs);
        Preconditions.checkNotNull(comparator);
        return xs.reduce((x, y) -> (comparator.compare(x, y) < 0) ? y : x);
    }

    public static <T> Observable<T> fromProcess(final ThrowingConsumer<Emitter<T>> process) {

        Preconditions.checkNotNull(process);

        return Observable.create(emitter -> {
            try {
                process.accept(emitter);
                emitter.onComplete();
            } catch (final Throwable e) {
                emitter.onError(e);
            }
        });
    }

    @Deprecated
    public static <T> Observable<T> fromAction(final Action action) {
        Preconditions.checkNotNull(action);
        return Observable.fromPublisher(subscriber -> {
            try {
                action.run();
                subscriber.onComplete();
            } catch (final Throwable throwable) {
                subscriber.onError(throwable);
            }
        });
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
                    .map(x -> ImmutableMap.of(entry.getKey(), x))
                ).collect(ImmutableList.toImmutableList())
        ).scan(initialValue, (x, y) -> MoreMaps.merge(x, y));
    }

    public static <A extends T, T> Observable<T> chain(final Observable<A> a, final Function<A, Observable<T>> f) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        return a.compose(PublishAndMergeTransformer.of(f));
    }

    public static <A extends T, B extends T, T> Observable<T> chain(
        final Observable<A> a, final Function<A, Observable<B>> f, final Function<B, Observable<T>> g) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        Objects.requireNonNull(g);
        return chain(a, i -> chain(f.apply(i), g));
    }

    public static <T> Observable<T> chainN(final Observable<T> a, final Function<T, Observable<T>>... fs) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(fs);
        return Arrays.stream(fs).reduce(
            a,
            (x, f) -> x.compose(PublishAndMergeTransformer.of(f)),
            Observable::concat);
    }

    @Deprecated
    public static <T> Observable<T> sequence(
        final Observable<? extends T> a, final Supplier<Observable<? extends T>> p) {

        final Subject<T> subject = PublishSubject.create();

        a.subscribe(
            subject::onNext,
            subject::onError,
            () -> {
                p.get().subscribe(
                    subject::onNext,
                    subject::onError,
                    subject::onComplete);
            });

        return subject;
    }
}
