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
            ImmutableList.of("a", "b", "Expected"),
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
        p.states().toList().blockingGet();

        assertEquals(2, (int) counter.value);
    }

    @Test
    public void countSubscribesForChain() throws Exception {

        final Mutable<Integer> counter = new Mutable<>(0);

        final Observable<Either<Integer, String>> o = Observable.just(left(1), left(2), left(3), right("A"));
        final Observable<Either<Integer, String>> m = o.doOnSubscribe(subscription -> {
            counter.value++;
        });

        final Process<Integer, String> a = Process.of(m);
        final Process<Integer, String> b = Process.chain(a, x -> Process.just("B"));

        b.result().blockingGet();

        assertEquals(1, (int) counter.value);
    }
}