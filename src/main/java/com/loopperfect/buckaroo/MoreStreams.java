package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class MoreStreams {

    private MoreStreams() {

    }

    public static <T, U> Predicate<T> distinctByKey(final Function<? super T, U> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        final Set<U> seen = new HashSet<>();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static <T> T single(final Stream<T> s) {
        Preconditions.checkNotNull(s);
        final List<T> xs = s.limit(2).collect(Collectors.toList());
        if (xs.isEmpty() || xs.size() > 1) {
            throw new IllegalStateException("Stream must have exactly one element");
        }
        return xs.get(0);
    }
}
