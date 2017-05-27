package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.function.Supplier;

public final class MoreObservables {

    private MoreObservables() {

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

    @Deprecated
    public static <T> Observable<T> sequence(final Observable<? extends T> a, final Supplier<Observable<? extends T>> p) {

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
