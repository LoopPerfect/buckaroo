package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.loopperfect.buckaroo.Either.left;
import static com.loopperfect.buckaroo.Either.right;
import static org.junit.Assert.*;

public final class ProcessTest {

    @Test
    public void basics1() throws Exception {

        final Process<Integer, String> p = Process.just("Hello", 1, 2, 3);

        assertEquals(ImmutableList.of(1, 2, 3), p.states().toList().blockingGet());
        assertEquals("Hello", p.result().blockingGet());
    }

    @Test
    public void basics2() throws Exception {

        final Process<Integer, String> p = Process.just("Hello");

        assertEquals(ImmutableList.of(), p.states().toList().blockingGet());
        assertEquals("Hello", p.result().blockingGet());
    }

    @Test
    public void basics3() throws Exception {

        final Observable<Either<Integer, ImmutableList<Integer>>> observable = Observable.just(
            right(ImmutableList.of(1, 2, 3)));

        final Process<Integer, List<Integer>> p = Process.of(observable);

        assertEquals(ImmutableList.of(1, 2, 3), p.result().blockingGet());
    }

    @Test
    public void invalid1() throws Exception {

        final Process<Integer, String> p = Process.of(Observable.just(right("Hello"), left(1)));

        assertEquals("Expected", p.result().onErrorReturnItem("Expected").blockingGet());
    }

    @Test
    public void invalid2() throws Exception {

        final Process<String, String> p = Process.of(Observable.just(right("Hello"), left("world")));

        assertEquals(ImmutableList.of("Expected"), p.states().onErrorReturnItem("Expected").toList().blockingGet());
    }

    @Test
    public void invalid3() throws Exception {

        final Process<String, String> p = Process.of(
            Observable.just(left("a"), left("b"), right("Hello"), left("c")));

        assertEquals(
            ImmutableList.of("a", "Expected"),
            p.states().onErrorReturnItem("Expected").toList().blockingGet());
    }

    @Test
    public void countSubscribes() throws Exception {

        final Mutable<Integer> counter = new Mutable<>(0);

        final Observable<Either<Integer, String>> o = Observable.just(left(1), left(2), left(3), right("Hello"));
        final Observable<Either<Integer, String>> m = o.doOnSubscribe(subscription -> {
            counter.value++;
        });

        final Process<Integer, String> p = Process.of(m);

        p.result().blockingGet();
        p.result().blockingGet();
        p.states().toList().blockingGet();
        p.states().toList().blockingGet();

        assertEquals(Integer.valueOf(1), counter.value);
    }

    @Test
    public void dontSwallowErrorsBeforeResult() throws Exception {

        Process<Integer, String> p = Process.of(Observable.create(s -> {
          s.onNext(left(1));
          s.onError(new Exception("error"));
          s.onNext(right("done"));
          s.onComplete();
        }));

        final int x = p.states().take(1).lastOrError().onErrorReturnItem(0).blockingGet();

        assertEquals(0, x);
    }

    @Test
    public void SingleFilter() throws ExecutionException, InterruptedException {
        SettableFuture<Boolean> f = SettableFuture.create();
        Single.just(true)
            .filter(x->x)
            .toObservable() // Apparently doesn't work without it, as maybes don't call onComplete in RxJava 2.x ?!?!?!
            .subscribe(x->{},e->{}, ()->{ f.set(true); });

        assertTrue(f.get());
    }


}