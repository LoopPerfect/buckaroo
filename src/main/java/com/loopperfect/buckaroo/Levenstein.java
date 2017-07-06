package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.javatuples.Pair;

import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * Original implementation by Ar90n
 * Created by gaetano on 04/07/17.
 */
public final class Levenstein {

    private Levenstein() {

    }

    private static int calcDistance(final String x, final String y) {

        Preconditions.checkNotNull(x);
        Preconditions.checkNotNull(y);

        final String minor = (x.length() < y.length()) ? x : y;
        final String major = (x.length() < y.length()) ? y : x;

        final int[] memo = IntStream.rangeClosed(0, minor.length()).toArray();
        for (char c : major.toCharArray()) {
            int prev = memo[0];
            memo[0]++;
            for (int j = 1; j <= minor.length(); ++j) {
                final int substitution = prev + (c == minor.charAt(j - 1) ? 0 : 1);
                prev = memo[j];
                memo[j] = Math.min(substitution, Math.min(memo[j - 1], memo[j]) + 1);
            }
        }

        return memo[minor.length()];
    }

    public static <T> Iterable<T> findClosest(final ImmutableList<T> candidates, final T misspelled) {

        final Comparator<Pair<T,Integer>> byDistance = Comparator.comparing(Pair::getValue1);

        return () -> candidates
            .stream()
            .map(c -> Pair.with(c, calcDistance(c.toString(), misspelled.toString())))
            .sorted(byDistance)
            .map(Pair::getValue0)
            .iterator();
    }
}
