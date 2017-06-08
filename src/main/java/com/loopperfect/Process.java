package com.loopperfect;

import com.loopperfect.buckaroo.Either;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.Objects;
import java.util.Optional;

public final class Process<S, T> {

    private final Observable<Either<S, T>> observable;

    private Process(final Observable<Either<S, T>> observable) {
        Objects.requireNonNull(observable, "observable is null");
        this.observable = observable;
    }

    public Observable<S> states() {
        return observable.map(x -> x.left())
            .takeWhile(Optional::isPresent)
            .map(Optional::get);
    }

    public Single<T> result() {
        return observable.map(x -> x.right())
            .skipWhile(x -> !x.isPresent())
            .map(Optional::get)
            .singleOrError();
    }

    public Observable<Either<S, T>> toObservable() {
        return observable;
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

    public static <S, T> Process<S, T> of(final Observable<Either<S, T>> observable) {
        return new Process<>(observable);
    }
}