package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import io.reactivex.Observer;

import java.util.concurrent.ExecutorService;

@Deprecated
public interface TaskContext<T> {

    ExecutorService executor();

    void onNext(final T next);

    void onError(final Throwable error);

    void onComplete();

    static <T> TaskContext<T> of(final ExecutorService executor, final Observer<T> observer) {

        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(observer);

        return new TaskContext<T>() {

            @Override
            public ExecutorService executor() {
                return executor;
            }

            @Override
            public void onNext(final T next) {
                observer.onNext(next);
            }

            @Override
            public void onError(final Throwable error) {
                observer.onError(error);
            }

            @Override
            public void onComplete() {
                observer.onComplete();
            }
        };
    }
}
