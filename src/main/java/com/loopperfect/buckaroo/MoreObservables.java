package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import io.reactivex.Emitter;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.*;
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
        return a.compose(PublishAndMergeTransformer.of(f));
    }

    public static <A extends T, B extends T, T> Observable<T> chain(
        final Observable<A> a, final Function<A, Observable<B>> f, final Function<B, Observable<T>> g) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        Objects.requireNonNull(g);
        return chain(a, i -> chain(f.apply(i), g));
    }

    public static <A extends T, B extends T, C extends T, T> Observable<T> chain(
        final Observable<A> a, final Function<A, Observable<B>> f, final Function<B, Observable<C>> g, final Function<C, Observable<T>> h) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(f);
        Objects.requireNonNull(g);
        Objects.requireNonNull(h);
        return chain(a, i -> chain(f.apply(i), g, h));
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
