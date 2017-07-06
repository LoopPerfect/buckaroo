package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.javatuples.Pair;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

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
    public void onErrorReturn() throws Exception {

        final Process<String, String> p = Process.of(
            Observable.concat(
                Observable.just(left("a"), left("b")),
                Observable.error(new IOException("Oh no!"))));

        assertEquals("error", p.onErrorReturn(throwable -> "error").result().blockingGet());
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
    public void chain1() throws Exception {

        final Process<Integer, String> a = Process.of(Observable.just(
            left(1), left(2), left(3), right("A")));

        final Process<Integer, String> b = Process.chain(a, x -> Process.just("B"));

        assertEquals("B", b.result().blockingGet());
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

    @Test
    public void chainN() throws Exception {

        final Process<Integer, Integer> x = Process.just(1, 1, 2, 3);
        final Process<Integer, Integer> y = Process.just(2, 4, 5, 6);
        final Process<Integer, Integer> z = Process.just(3, 7, 8, 9);

        final ImmutableList<Function<Integer, Process<Integer, Integer>>> xs = ImmutableList.of(
            i -> y.mapStates(j -> i + j),
            i -> z.mapStates(j -> i + j));

        final Process<Integer, Integer> w = Process.chainN(x, xs);

        final List<Integer> expected = ImmutableList.of(
            1, 2, 3,
            5, 6, 7,
            9, 10, 11);

        final List<Integer> actual = w.states().toList().blockingGet();

        assertEquals(expected, actual);
    }

    @Test
    public void chainNDeep() throws Exception {

        final Process<Integer, Integer> x = Process.of(Observable.just(
            left(1), left(2), left(3), right(0)));

        final int n = 1024;

        final ImmutableList<Function<Integer, Process<Integer, Integer>>> xs = IntStream.range(0, n)
            .mapToObj(i -> (Function<Integer, Process<Integer, Integer>>) integer -> Process.just(integer + 1))
            .collect(ImmutableList.toImmutableList());

        final Process<Integer, Integer> y = Process.chainN(x, xs);

        assertEquals(n, (int)y.result().blockingGet());
    }

    @Test
    public void countSubscribesForChainN() throws Exception {

        final AtomicInteger counter = new AtomicInteger(0);

        final Process<Integer, Integer> x = Process.of(Observable.just(
            left(1), left(2), left(3), right(0)));

        final int n = 1024;

        final ImmutableList<Function<Integer, Process<Integer, Integer>>> xs = IntStream.range(0, n)
            .mapToObj(i -> (Function<Integer, Process<Integer, Integer>>) integer -> {
                final Observable<Either<Integer, Integer>> o = Observable.just(Either.right(integer + 1));
                return Process.of(o.doOnSubscribe(subscription -> {
                    counter.incrementAndGet();
                }));
            })
            .collect(ImmutableList.toImmutableList());

        final Process<Integer, Integer> y = Process.chainN(x, xs);

        y.result().blockingGet();

        assertEquals(n, counter.intValue());
    }

    @Test
    public void merge1() throws Exception {

        final Process<Integer, String> a = Process.of(Observable.just(
            Either.left(1), Either.left(2), Either.left(3), Either.right("A")));

        final Process<Integer, String> b = Process.of(Observable.just(
            Either.left(7), Either.left(8), Either.left(9), Either.right("B")));

        final CountDownLatch latch = new CountDownLatch(2);

        Process.merge(a, b).result().subscribe(result -> {
            assertEquals(Pair.with("A", "B"), result);
            latch.countDown();
        });

        Process.merge(a, b).states().subscribe(next -> {}, error -> {}, latch::countDown);

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void merge2() throws Exception {

        final ImmutableList<Process<Integer, String>> xs = ImmutableList.of(
            Process.of(Observable.just(
                Either.left(1), Either.left(2), Either.left(3), Either.right("A"))),
            Process.of(Observable.just(
                Either.left(4), Either.left(5), Either.left(6), Either.right("B"))),
            Process.of(Observable.just(
                Either.left(7), Either.left(8), Either.left(9), Either.right("C"))));

        final CountDownLatch latch = new CountDownLatch(2);

        Process.merge(xs).result().subscribe(result -> {
            assertEquals(ImmutableList.of("A", "B", "C"), result);
            latch.countDown();
        });

        Process.merge(xs).states().toList().subscribe(result -> {
            assertEquals(3 * 3, result.size());
            latch.countDown();
        });

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void merge3() throws Exception {

        final Process<Integer, String> a = Process.of(Observable.just(
            Either.left(1), Either.left(2), Either.left(3), Either.right("A")));

        final Process<Integer, String> b = Process.of(Observable.error(new IOException("!")));

        final CountDownLatch latch = new CountDownLatch(1);

        Process.merge(a, b).states().subscribe(
            next -> {},
            error -> {
                assertTrue(error instanceof IOException);
                assertEquals("!", error.getMessage());
                latch.countDown();
            },
            () -> {});

        latch.await(5000L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void countMergeSubscribes() throws Exception {

        final Mutable<Integer> counter = new Mutable<>(0);

        final Observable<Either<Integer, String>> o = Observable.just(
            Either.left(1), Either.left(2), Either.left(3), Either.right("A"));

        final Process<Integer, String> a = Process.of(o.doOnSubscribe(subscription -> {
            counter.value++;
        }));

        final Process<Integer, String> b = Process.of(Observable.just(
            Either.left(7), Either.left(8), Either.left(9), Either.right("B")));

        final CountDownLatch latch = new CountDownLatch(1);

        Process.merge(a, b).result().subscribe(result -> {
            latch.countDown();
        });

        latch.await(5000L, TimeUnit.MILLISECONDS);

        assertEquals(1, (int)counter.value);
    }

    @Test
    public void max() throws Exception {

        final Process<Integer, Integer> a = Process.just(1);
        final Process<Integer, Integer> b = Process.just(2);

        final Process<Integer, Integer> c = Process.max(a, b, Integer::compareTo);

        assertEquals(2, (int) c.result().blockingGet());
    }

    @Test
    public void maxN() throws Exception {

        final Process<Integer, Integer> x = Process.just(1);
        final ImmutableList<Process<Integer, Integer>> xs = ImmutableList.of(
            Process.just(1),
            Process.just(3),
            Process.just(2),
            Process.just(2));

        final Process<Integer, Integer> c = Process.max(x, xs, Integer::compareTo);

        assertEquals(3, (int) c.result().blockingGet());
    }

    @Test
    public void maxCountSubscribes() throws Exception {

        final Mutable<Integer> counter = new Mutable<>(0);

        final Process<Integer, Integer> x = Process.of(Single.just(4)
            .doOnSubscribe(subscription -> counter.value++));

        final ImmutableList<Process<Integer, Integer>> xs = ImmutableList.of(
            Process.of(Single.just(1)
                .doOnSubscribe(subscription -> counter.value++)),
            Process.of(Single.just(2)
                .doOnSubscribe(subscription -> counter.value++)),
            Process.of(Single.just(3)
                .doOnSubscribe(subscription -> counter.value++)));

        final Process<Integer, Integer> c = Process.max(x, xs, Integer::compareTo);

        assertEquals(4, (int) c.result().blockingGet());
        assertEquals(4, (int) counter.value);
    }
}