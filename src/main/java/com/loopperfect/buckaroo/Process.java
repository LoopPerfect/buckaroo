package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function4;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class Process<S, T> {

    private final Observable<Either<S, T>> observable;

    private Process(final Observable<Either<S, T>> observable) {
        Objects.requireNonNull(observable, "observable is null");
        this.observable = observable.cache();
    }

    public Observable<S> states() {
        return Observable.concat(
            observable.takeWhile(x -> x.left().isPresent()),
            observable.skipWhile(x -> !x.right().isPresent())
                .singleOrError()
                .onErrorResumeNext(error ->
                    Single.error(new ProcessException("A process must have a result. ", error)))
                .toObservable())
            .skipLast(1)
            .map(x -> x.left().get());
    }

    public Single<T> result() {
        return observable.map(x -> x.right())
            .skipWhile(x -> !x.isPresent())
            .map(Optional::get)
            .singleOrError()
            .onErrorResumeNext(error ->
                Single.error(new ProcessException("A process must have a result. ", error)));
    }

    public Observable<Either<S, T>> toObservable() {
        return observable;
    }

    public <U> Process<S, U> map(final Function<T, U> f) {
        Objects.requireNonNull(f, "f is null");
        return Process.of(observable.map(x -> x.rightMap(f)));
    }

    public <U> Process<U, T> mapStates(final Function<S, U> f) {
        Objects.requireNonNull(f, "f is null");
        return Process.of(observable.map(x -> x.leftMap(f)));
    }

    public <U> Process<S, U> chain(final Function<T, Process<S, U>> f) {
        Objects.requireNonNull(f, "f is null");
        return Process.chain(this, f);
    }

    public static <S, T> Process<S, T> just(final T result) {
        return of(Observable.just(Either.right(result)));
    }

    @SafeVarargs
    public static <S, T> Process<S, T> just(final T result, S... states) {
        return of(Observable.concat(
            Observable.fromArray(states).map(Either::left),
            Observable.just(Either.right(result))));
    }

    public static <S, T> Process<S, T> error(final Throwable error) {
        return of(Observable.error(error));
    }

    public static <S, T, A extends S, B extends T> Process<S, T> of(final Observable<Either<A, B>> observable) {
        return new Process<>(observable.map(x -> x.flatMap(Either::left, Either::right)));
    }

    public static <S, T> Process<S, T> of(final Observable<S> states, final Single<T> result) {
        return new Process<>(Observable.concat(
            states.map(Either::left),
            result.toObservable().map(Either::right)
        ));
    }

    public static <S> Process<S, S> usingLastAsResult(final Observable<S> observable) {
        return new Process<>(Observable.concat(
            observable.map(Either::left),
            observable.lastOrError().map(Either::<S, S>right).toObservable()
        ));
    }

    public static <S, T> Process<S, T> of(final Single<T> single) {
        return of(single.map(Either::<S, T>right).toObservable());
    }

    public static <S, T> Process<S, T> of(final Single<T> single, final Class<S> stateClass) {
        Objects.requireNonNull(single, "single is null");
        Objects.requireNonNull(stateClass, "single is null");
        return of(single.map(Either::<S, T>right).toObservable());
    }

    public static <A extends S, X extends S, S, T> Process<S, T> concat(final Process<A, ?> a, final Process<X, T> b) {
        return of(Observable.concat(
            a.states().map(Either::<S, T>left),
            b.toObservable()
                .delaySubscription(a.result().toObservable())
                .map(x -> Either.join(x, Either::left, Either::right))
        ));
    }

    public static <S, T> Process<S, T> concat(final Process<S, T> x, final Iterable<Process<S, T>> xs) {
        Objects.requireNonNull(x, "x is null");
        Objects.requireNonNull(xs, "xs is null");
        return Streams.stream(xs).reduce(x, Process::concat);
    }

    public static <S, T, U> Process<S, U> chain(final Process<S, T> x, final Function<T, Process<S, U>> f) {
        Objects.requireNonNull(x, "x is null");
        Objects.requireNonNull(f, "f is null");
        return of(Observable.concat(
            x.states().map(Either::left),
            x.result().flatMapObservable(i -> f.apply(i).toObservable())
        ));
    }

    public static <S, A, B, C> Process<S, C> chain(
        final Process<S, A> x, final Function<A, Process<S, B>> f, final Function<B, Process<S, C>> g) {

        Objects.requireNonNull(x, "x is null");
        Objects.requireNonNull(f, "f is null");
        Objects.requireNonNull(g, "g is null");

        return chain(chain(x, f), g);
    }

    public static <S, T> Process<S, T> chainN(final Process<S, T> a, final Function<T, Process<S, T>>... fs) {

        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(fs);

        return Arrays.stream(fs).reduce(
            a,
            (x, f) -> Process.chain(x, f),
            Process::concat);
    }

    public static <S, T> Process<S, T> chainN(final Process<S, T> a, final Iterable<Function<T, Process<S, T>>> fs) {

        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(fs);

        return Streams.stream(fs).reduce(
            a,
            (x, f) -> Process.chain(x, f),
            Process::concat);
    }
}
