package com.loopperfect.buckaroo;

import io.reactivex.Flowable;
import io.reactivex.Single;

import java.util.concurrent.Callable;

public final class MoreSingles {

    private MoreSingles() {

    }

    public static <T> Single<T> fromCallableWithDispose(final Callable<T> callable) {
        return Single.create(e -> {
            if (e.isDisposed()) {
                return;
            }
            try {
                final T result = callable.call();
                if (e.isDisposed()) {
                    return;
                }
                e.onSuccess(result);
            } catch (final Throwable error) {
                if (e.isDisposed()) {
                    return;
                }
                e.onError(error);
            }
        });
    }
}
