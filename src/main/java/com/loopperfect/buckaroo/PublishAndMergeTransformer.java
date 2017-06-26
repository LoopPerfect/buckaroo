package com.loopperfect.buckaroo;

import io.reactivex.*;

import java.util.Objects;
import java.util.function.Function;

/**
 * This transformer chains two Observables together, where the second Observable
 * is a function of the final value of the first.
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public final class PublishAndMergeTransformer<A extends C, B extends C, C> implements ObservableTransformer<A, C> {

    private final Function<A, Observable<B>> f;

    public PublishAndMergeTransformer(final Function<A, Observable<B>> f) {
        Objects.requireNonNull(f, "f is null");
        this.f = f;
    }

    @Override
    public ObservableSource<C> apply(final Observable<A> x) {
        Objects.requireNonNull(x, "x is null");
        return x.publish(i -> Observable.concat(
            i,
            i.lastOrError()
             .onErrorResumeNext(error ->
                 Single.error(new RuntimeException("PublishAndMerge requires a non-empty Observable", error)))
             .toObservable()
             .flatMap(f::apply)));
    }

    public static <A extends C, B extends C, C> ObservableTransformer<A, C> of(final Function<A, Observable<B>> f) {
        Objects.requireNonNull(f, "f is null");
        return new PublishAndMergeTransformer<>(f);
    }

    public static <A extends C, B extends C, C> ObservableTransformer<A, C> ofSingle(final Function<A, Single<B>> f) {
        Objects.requireNonNull(f, "f is null");
        return new PublishAndMergeTransformer<>(f.andThen(Single::toObservable));
    }
}
