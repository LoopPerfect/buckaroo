package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;

import java.util.concurrent.ExecutorService;

@Deprecated
public final class Tasks {

    private Tasks() {

    }

    public static <T> Observable<T> run(final ListeningExecutorService executor, final Task<T> task) {

        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(task);

        final Subject<T> unsafeSubject = ReplaySubject.create();
        final Subject<T> subject = unsafeSubject.toSerialized();

        Futures.addCallback(
            executor.submit(() -> {
                task.run(subject);
            }),
            new FutureCallback<Object>() {

                @Override
                public void onSuccess(final Object v) {

                }

                @Override
                public void onFailure(final Throwable throwable) {
                    // We need to catch the case where the executor is shutdown.
                    subject.onError(throwable);
                }
            });

        return subject;
    }

    public static <T> Observable<T> run(final ExecutorService executor, final Task<T> task) {
        return run(MoreExecutors.listeningDecorator(executor), task);
    }

    public static <T> Observable<T> run(final Task<T> task) {
        return run(MoreExecutors.newDirectExecutorService(), task);
    }

    public static <T> Task<T> sequence(final Task<T> a, final Task<T> b) {

        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);

        return observer -> {

            Preconditions.checkNotNull(observer);

            final Subject<T> subject = PublishSubject.create();

            subject.subscribe(
                observer::onNext,
                observer::onError,
                () -> {
                    final Subject<T> y = PublishSubject.create();

                    y.subscribe(
                        observer::onNext,
                        observer::onError,
                        observer::onComplete);

                    b.run(y);
                });

            a.run(subject);
        };
    }
}
