package com.loopperfect.buckaroo;

import io.reactivex.*;
import io.reactivex.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * This transformer chains two Observables together, where the second Observable
 * is a function of the final value of the first.
 *
 * The first Observable must have at least one element.
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
        return Observable.create(new ObservableOnSubscribe<C>() {

            transient volatile Optional<A> last = Optional.empty();

            private void sendPrevious(@NonNull final ObservableEmitter<C> e) {
                if (last.isPresent()) {
                    e.onNext(last.get());
                }
            }

            @Override
            public void subscribe(@NonNull final ObservableEmitter<C> e) throws Exception {
                x.subscribe(next -> {
                    sendPrevious(e);
                    last = Optional.of(next);
                }, error -> {
                    sendPrevious(e);
                    e.onError(error);
                }, () -> {
                    sendPrevious(e);
                    if (last.isPresent()) {
                        f.apply(last.get()).subscribe(e::onNext, e::onError, e::onComplete);
                    } else {
                        e.onComplete();
                    }
                });
            }
        });
    }

    public static <A extends C, B extends C, C> ObservableTransformer<A, C> of(final Function<A, Observable<B>> f) {
        Objects.requireNonNull(f, "f is null");
        return new PublishAndMergeTransformer<>(f);
    }
}
