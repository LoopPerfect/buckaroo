package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import io.reactivex.Observer;

import java.util.function.Supplier;

@Deprecated
@FunctionalInterface
public interface Task<T> {

    void run(final Observer<T> observer);

    static <T> Task<T> of(final Supplier<T> f) {
        Preconditions.checkNotNull(f);
        return context -> {
            Preconditions.checkNotNull(context);
            try {
                final T result = f.get();
                context.onNext(result);
                context.onComplete();
            } catch (final Throwable throwable) {
                context.onError(throwable);
            }
        };
    }
}
