package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MoreStreams {

    private MoreStreams() {

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
