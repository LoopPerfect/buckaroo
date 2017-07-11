package com.loopperfect.buckaroo;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

public final class StrictOrderingComposition<T> implements ObservableTransformer<T, T> {

    @Override
    public ObservableSource<T> apply(@NonNull final Observable<T> upstream) {
        final Object lock = new Object();
        final AtomicBoolean isDone = new AtomicBoolean(false);
        return Observable.create(emitter -> {
            upstream.subscribe(next -> {
                if (isDone.get() || emitter.isDisposed()) {
                    return;
                }
                synchronized (lock) {
                    if (!isDone.get() && !emitter.isDisposed()) {
                        emitter.onNext(next);
                    }
                }
            }, error -> {
                if (isDone.get() || emitter.isDisposed()) {
                    return;
                }
                synchronized (lock) {
                    if (!isDone.get() && !emitter.isDisposed()) {
                        isDone.set(true);
                        emitter.onError(error);
                    }
                }
            }, () -> {
                if (isDone.get() || emitter.isDisposed()) {
                    return;
                }
                synchronized (lock) {
                    if (!isDone.get() && !emitter.isDisposed()) {
                        emitter.onComplete();
                    }
                }
            });
        });
    }
}
