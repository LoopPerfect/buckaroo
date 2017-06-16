package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Observable;
import org.junit.Test;

import java.util.List;
import java.util.Map;

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
}