package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.reactivex.Observable;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public final class MoreObservablesTest {

    @Test
    public void zipMaps() throws Exception {

        final Map<String, Observable<Integer>> o = ImmutableMap.of("a", Observable.just(1, 2, 3));

        final List<Map<String, Integer>> expected = ImmutableList.of(
            ImmutableMap.of("a", 1),
            ImmutableMap.of("a", 2),
            ImmutableMap.of("a", 3));

        final List<Map<String, Integer>> actual = MoreObservables.zipMaps(o).toList().blockingGet();

        assertEquals(expected, actual);
    }

    @Test
    public void zipMapsWithEmpty() throws Exception {

        final Map<String, Observable<Integer>> o = ImmutableMap.of(
            "a", Observable.just(1, 2, 3).delay(1, TimeUnit.MILLISECONDS),
            "b", Observable.just(1, 2, 3),
            "c", Observable.empty()
        );

        final ImmutableSet<Map<String, Integer>> expected = ImmutableSet.of();

        final ImmutableSet<Map<String, Integer>> actual = ImmutableSet.copyOf(MoreObservables.zipMaps(o)
            .toList()
            .blockingGet());

        assertEquals(expected , actual);
    }

    @Test
    public void mergeMaps() throws Exception {

        final Map<String, Observable<Integer>> o = ImmutableMap.of(
            "a", Observable.just(1),
            "b", Observable.just(1, 2, 3),
            "c", Observable.empty()
        );

        final ImmutableMap<String, Integer> expected = ImmutableMap.of(
            "a", 1,
            "b", 3
        );

        final ImmutableMap<String, Integer> actual = MoreObservables.mergeMaps(o)
            .lastElement()
            .blockingGet();

        assertEquals(expected , actual);
    }

    @Test
    public void exceptionsShouldBeDeliverableWithMergedMaps() throws Exception {

        Observable<Integer> a = Observable.create(s -> {
            s.onNext(1);
            s.onError(new Exception("error"));
            s.onNext(2);
            s.onComplete();
        });

        Observable<Integer> b = Observable.create(s -> {
            s.onNext(1);
            s.onNext(2);
            s.onNext(3);
            s.onError(new Exception("error"));
            s.onComplete();
        });

        final ImmutableMap<String, Observable<Integer>> map = ImmutableMap.of(
            "a", a,
            "b", b
        );

        final Observable<ImmutableMap<String, Integer>> observableMap
            = MoreObservables.mergeMaps(map);

        final int error = observableMap
            .reduce(0, (x, y) -> 0)
            .onErrorReturnItem(1)
            .blockingGet();

        assertEquals(1, error);
    }

    @Test
    public void exceptionsShouldBeDeliverableWithMergedMaps2() throws Exception {

        final Observable<Integer> a = Observable.error(new IOException("blub"));

        final Observable<Integer> b = Observable.error(new IOException("blub"));

        final ImmutableMap<String, Observable<Integer>> map = ImmutableMap.of(
            "a", a,
            "b", b
        );

        final Observable<ImmutableMap<String, Integer>> observableMap
            = MoreObservables.mergeMaps(map);

        final int error = observableMap
            .reduce(0, (x, y) -> 0)
            .onErrorReturnItem(1)
            .blockingGet();

        assertEquals(1, error);
    }

    @Test
    public void shouldHandleEmptyInMergedMaps() throws Exception {

        final Observable<Integer> a = Observable.empty();

        final Observable<Integer> b = Observable.create(s -> {
            s.onNext(1);
            s.onNext(2);
            s.onNext(3);
            s.onComplete();
        });

        final ImmutableMap<String, Observable<Integer>> map = ImmutableMap.of(
            "a", a,
            "b", b
        );

        final Observable<ImmutableMap<String, Integer>> observableMap =
            MoreObservables.mergeMaps(map);

        final int error = observableMap
            .reduce(1, (x, y) -> 0)
            .blockingGet();

        assertEquals(0, error);
    }
}