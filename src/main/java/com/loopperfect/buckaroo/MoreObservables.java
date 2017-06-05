package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    @SuppressWarnings("unchecked")
    public static <T> Observable<List<T>> parallel(final Iterable<Observable<T>> xs) {
        Preconditions.checkNotNull(xs);
        return Observable.zip(
            xs,
            objects -> Arrays.stream(objects)
                .map(x -> (T) x)
                .collect(toImmutableList())
        );
    }

    public static <A extends T, T> Observable<T> chain(final Observable<A> a, final Function<A, Observable<T>> f) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        return Observable.concat(
            a,
            a.takeLast(1).flatMap(f::apply)
        );
    }

    public static <A extends T, B extends T, T> Observable<T> chain(
        final Observable<A> a, final Function<A, Observable<B>> f, final Function<B, Observable<T>> g) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        Objects.requireNonNull(g);
        return Observable.concat(
            a,
            a.takeLast(1).flatMap(x -> chain(f.apply(x), g))
        );
    }

    public static <A extends T, B extends T, C extends T, T> Observable<T> chain(
        final Observable<A> a, final Function<A, Observable<B>> f, final Function<B, Observable<C>> g, final Function<C, Observable<T>> h) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        Objects.requireNonNull(g);
        Objects.requireNonNull(h);
        return Observable.concat(
            a,
            a.takeLast(1).flatMap(x -> chain(f.apply(x), g, h))
        );
    }

    public static <T> Observable<T> chain(final Observable<T> a, final Function<T, Observable<T>>... fs) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(fs);
        return Arrays.stream(fs).reduce(
            a,
            (x, f) -> Observable.concat(x, x.takeLast(1).flatMap(f::apply)),
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
