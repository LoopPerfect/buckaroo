package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import io.reactivex.Observable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
            Either.right(ImmutableList.of(1, 2, 3)));

        final Process<Integer, List<Integer>> p = Process.of(observable);

        assertEquals(ImmutableList.of(1, 2, 3), p.result().blockingGet());
    }

    @Test
    public void invalid1() throws Exception {

        final Process<Integer, String> p = Process.of(Observable.just(Either.right("Hello"), Either.left(1)));

        assertEquals("Expected", p.result().onErrorReturnItem("Expected").blockingGet());
    }

    @Test
    public void invalid2() throws Exception {

        final Process<String, String> p = Process.of(Observable.just(Either.right("Hello"), Either.left("world")));

        assertEquals(ImmutableList.of("Expected"), p.states().onErrorReturnItem("Expected").toList().blockingGet());
    }
}