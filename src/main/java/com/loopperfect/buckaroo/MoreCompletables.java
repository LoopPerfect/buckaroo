package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.tasks.UnsafeRunnable;
import io.reactivex.Completable;

public final class MoreCompletables {

    private MoreCompletables() {

    }

    public static Completable fromRunnable(final UnsafeRunnable runnable) {
        Preconditions.checkNotNull(runnable);
        return Completable.fromPublisher(subscriber -> {
            try {
                runnable.run();
                subscriber.onComplete();
            } catch (final Throwable throwable) {
                subscriber.onError(throwable);
            }
        });
    }
}
