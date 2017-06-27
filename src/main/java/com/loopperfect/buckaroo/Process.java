package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import io.reactivex.*;
import io.reactivex.annotations.NonNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.loopperfect.buckaroo.Either.*;
import static com.loopperfect.buckaroo.Either.right;

public final class Process<S, T> {

    private final Observable<Either<S, T>> observable;

    private Process(final Observable<Either<S, T>> observable) {
        Objects.requireNonNull(observable, "observable is null");
        this.observable = observable;
    }

    public Observable<S> states() {
        return Observable.create(new ObservableOnSubscribe<S>() {
            private boolean isComplete = false;
            @Override
            public void subscribe(@NonNull final ObservableEmitter<S> emitter) throws Exception {
                observable.subscribe(next -> {
                    if (isComplete) {
                        emitter.onError(new ProcessException("Process must push exactly one result. "));
                    } else {
                        if (next.isRight()) {
                            isComplete = true;
                        } else {
                            emitter.onNext(next.left().get());
                        }
                    }
                }, emitter::onError, emitter::onComplete);
            }
        });
    }

    public Single<T> result() {
        return Single.create(new SingleOnSubscribe<T>() {
            private Optional<T> t = Optional.empty();
            @Override
            public void subscribe(@NonNull final SingleEmitter<T> emitter) throws Exception {
                observable.subscribe(next -> {
                    if (t.isPresent()) {
                        emitter.onError(new ProcessException("Process must not push states after the result. "));
                    } else {
                        if (next.isRight()) {
                            t = next.right();
                        }
                    }
                }, emitter::onError, () -> {
                    if (t.isPresent()) {
                        emitter.onSuccess(t.get());
                    } else {
                        emitter.onError(new ProcessException("Process must push a result. "));
                    }
                });
            }
        });
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
        return of(Observable.just(right(result)));
    }

    @SafeVarargs
    public static <S, T> Process<S, T> just(final T result, S... states) {
        return of(Observable.concat(
            Observable.fromArray(states).map(Either::left),
            Observable.just(right(result))));
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
                .map(x -> join(x, Either::left, Either::right))
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

        final Observable<Either<S, U>> o = Observable.create(new ObservableOnSubscribe<Either<S, U>>() {

            private Optional<S> lastState = Optional.empty();
            private Optional<T> result = Optional.empty();

            private void sendLastState(@NonNull final ObservableEmitter<Either<S, U>> emitter) {
                if (lastState.isPresent()) {
                    emitter.onNext(left(lastState.get()));
                }
            }

            @Override
            public void subscribe(@NonNull final ObservableEmitter<Either<S, U>> emitter) throws Exception {
                x.toObservable().subscribe((Either<S, T> next) -> {
                    if (result.isPresent()) {
                        emitter.onError(new ProcessException("A process must not have more than one result"));
                    } else {
                        sendLastState(emitter);
                        if (next.isRight()) {
                            result = next.right();
                        } else {
                            lastState = next.left();
                        }
                    }
                }, emitter::onError, () -> {
                    if (result.isPresent()) {
                        final Observable<Either<S, U>> nextObservable = f.apply(result.get()).toObservable();
                        nextObservable.subscribe(emitter::onNext, emitter::onError, emitter::onComplete);
                    } else {
                        emitter.onError(new ProcessException("A process must have a result. "));
                    }
                });
            }
        });
        return Process.of(o);
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
