package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.SettableFuture;
import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import org.javatuples.Pair;

import java.util.*;
import java.util.function.Function;

import static com.loopperfect.buckaroo.Either.*;

public final class Process<S, T> {

    private final Observable<Either<S, T>> observable;

    private Process(final Observable<Either<S, T>> observable) {
        Objects.requireNonNull(observable, "observable is null");
        this.observable = observable;
    }

    public Observable<S> states() {

        final Object lock = new Object();

        return Observable.create(new ObservableOnSubscribe<S>() {

            private boolean isComplete = false;

            @Override
            public void subscribe(@NonNull final ObservableEmitter<S> emitter) throws Exception {
                observable.subscribe(
                    next -> {
                        if (isComplete) {
                            if (emitter.isDisposed()) {
                                return;
                            }
                            synchronized (lock) {
                                if (!emitter.isDisposed()) {
                                    emitter.onError(new ProcessException("Process must push exactly one result. "));
                                }
                            }
                        } else {
                            if (next.isRight()) {
                                isComplete = true;
                            } else {
                                emitter.onNext(next.left().get());
                            }
                        }
                    },
                    error -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        synchronized (lock) {
                            if (!emitter.isDisposed()) {
                                emitter.onError(error);
                            }
                        }
                    },
                    () -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        emitter.onComplete();
                    });
            }
        });
    }

    public Single<T> result() {
        final Object lock = new Object();
        return Single.create(emitter -> {
            final Mutable<Optional<T>> result = new Mutable<>(Optional.empty());
            observable.subscribe(
                next -> {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    if (result.value.isPresent()) {
                        synchronized (lock) {
                            if (!emitter.isDisposed()) {
                                emitter.onError(new ProcessException("Process must not push states after the result. "));
                            }
                        }
                    } else {
                        if (next.isRight()) {
                            result.value = next.right();
                        }
                    }
                },
                error -> {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    synchronized (lock) {
                        if (!emitter.isDisposed()) {
                            emitter.onError(error);
                        }
                    }
                },
                () -> {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    if (result.value.isPresent()) {
                        emitter.onSuccess(result.value.get());
                    } else {
                        synchronized (lock) {
                            if (!emitter.isDisposed()) {
                                emitter.onError(new ProcessException("Process must push a result. "));
                            }
                        }
                    }
                });
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

    public Process<S, T> onErrorReturn(final Function<Throwable, T> f) {
        Objects.requireNonNull(f, "f is null");
        return Process.of(toObservable().onErrorReturn(x -> right(f.apply(x))));
    }

    public Process<S, T> mapErrors(final Function<Throwable, Throwable> f) {
        Objects.requireNonNull(f, "f is null");
        return Process.of(toObservable().onErrorResumeNext((Throwable e) -> Observable.error(f.apply(e))));
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
                }, error -> {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    emitter.onError(error);
                }, () -> {
                    if (result.isPresent()) {
                        final Observable<Either<S, U>> nextObservable = f.apply(result.get()).toObservable();
                        nextObservable.subscribe(emitter::onNext, emitter::onError, emitter::onComplete);
                    } else {
                        emitter.onError(new ProcessException("A process must have a result. "));
                    }
                }, subscription -> {
                    if (emitter.isDisposed() && !subscription.isDisposed()) {
                        subscription.dispose();
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

        return chainN(a, ImmutableList.copyOf(fs));
    }

    public static <S, T> Process<S, T> chainN(final Process<S, T> x, final Iterable<Function<T, Process<S, T>>> fs) {

        Preconditions.checkNotNull(x);
        Preconditions.checkNotNull(fs);

        // The logic here is a bit strange because we need to prevent stack-overflow errors for large chains.
        // The strategy is to compose everything into a list of observables which can then be merged using
        // Rx's built-in merge function that implements a trampoline.
        // In order to chain the processes without causing extra subscribes, we put a side-effect into the each process
        // that sets a future containing its result.
        // The future is then used as the trigger for the next process in the chain.

        // Use an ArrayList as a builder.
        final List<Observable<Either<S, T>>> ys = Lists.newArrayList();

        Observable<Either<S, T>> previous = x.toObservable();

        for (final Function<T, Process<S, T>> f : fs) {

            final SettableFuture<T> future = SettableFuture.create();

            // When the previous process completes, we should put the result into a future.
            // We skip the last value because we only want the states for processes inside the chain.
            previous = previous.doOnNext(i -> {
                if (i.isRight()) {
                    future.set(i.right().get());
                }
            }).skipLast(1);

            ys.add(previous);

            // The next process is then generated from the future.
            previous = Single.fromFuture(future)
                .flatMapObservable(i -> f.apply(i).toObservable());
        }

        ys.add(previous);

        return Process.of(Observable.merge(ys));
    }

    /**
     * Takes two Process objects and merges them into one.
     * <p>
     * The semantics are:
     * - The resulting process only finishes when both complete
     * - All events from both processes are pushed
     * - An error in either process causes an error in the merged process
     * - A subscription to the merged process triggers a subscription on each child process
     *
     * @param a    The first process to merge
     * @param b    The second process to merge
     * @param <S>  The type of the state objects in both processes
     * @param <T1> The result type of the first process
     * @param <T2> The result type of the second process
     * @return
     */
    public static <S, T1, T2> Process<S, Pair<T1, T2>> merge(final Process<? extends S, T1> a, final Process<? extends S, T2> b) {

        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);

        final Observable<Either<S, Pair<T1, T2>>> o = Observable.create(emitter -> {

            final Mutable<Optional<T1>> resultA = new Mutable<>(Optional.empty());
            final Mutable<Optional<T2>> resultB = new Mutable<>(Optional.empty());

            a.toObservable().subscribe(
                next -> {
                    if (next.isLeft()) {
                        emitter.onNext(Either.left(next.left().get()));
                    } else {
                        resultA.value = next.right();
                    }
                },
                error -> {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    emitter.onError(error);
                },
                () -> {
                    if (!emitter.isDisposed() && resultA.value.isPresent() && resultB.value.isPresent()) {
                        emitter.onNext(Either.right(Pair.with(resultA.value.get(), resultB.value.get())));
                        emitter.onComplete();
                    }
                },
                subscription -> {
                    if (emitter.isDisposed() && !subscription.isDisposed()) {
                        subscription.dispose();
                    }
                });

            b.toObservable().subscribe(
                next -> {
                    if (next.isLeft()) {
                        emitter.onNext(Either.left(next.left().get()));
                    } else {
                        resultB.value = next.right();
                    }
                },
                error -> {
                    if (emitter.isDisposed()) {
                        return;
                    }
                    emitter.onError(error);
                },
                () -> {
                    if (!emitter.isDisposed() && resultA.value.isPresent() && resultB.value.isPresent()) {
                        emitter.onNext(Either.right(Pair.with(resultA.value.get(), resultB.value.get())));
                        emitter.onComplete();
                    }
                },
                subscription -> {
                    if (emitter.isDisposed() && !subscription.isDisposed()) {
                        subscription.dispose();
                    }
                });
        });

        return Process.of(o);
    }

    public static <S, T> Process<S, ImmutableList<T>> merge(final ImmutableList<Process<S, T>> xs) {
        Objects.requireNonNull(xs);
        return xs.stream()
            .map(i -> i.map(ImmutableList::of))
            .reduce(
                Process.just(ImmutableList.of()),
                (i, j) -> Process.merge(i, j).map(k -> MoreLists.concat(k.getValue0(), k.getValue1())));
    }

    public static <S, T> Process<S, T> max(final Process<S, T> a, final Process<S, T> b, final Comparator<T> comparator) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Objects.requireNonNull(comparator);
        return merge(a, b).map(xy ->
            comparator.compare(xy.getValue0(), xy.getValue1()) > 0 ?
                xy.getValue0() :
                xy.getValue1());
    }

    public static <S, T> Process<S, T> max(final Process<S, T> x, final Collection<Process<S, T>> xs, final Comparator<T> comparator) {
        Objects.requireNonNull(xs, "xs is null");
        Objects.requireNonNull(comparator, "comparator is null");
        return xs.stream()
            .reduce(x, (state, next) -> Process.max(state, next, comparator));
    }

    public static <S, T> Optional<Process<S, T>> max(final Collection<Process<S, T>> xs, final Comparator<T> comparator) {
        Objects.requireNonNull(xs, "xs is null");
        Objects.requireNonNull(comparator, "comparator is null");
        return xs.stream()
            .reduce((state, next) -> Process.max(state, next, comparator));
    }

    public static <S, T> Maybe<Process<S, T>> max(final Observable<Process<S, T>> xs, final Comparator<T> comparator) {
        Objects.requireNonNull(xs, "xs is null");
        Objects.requireNonNull(comparator, "comparator is null");
        return xs.reduce((state, next) -> Process.max(state, next, comparator));
    }
}
