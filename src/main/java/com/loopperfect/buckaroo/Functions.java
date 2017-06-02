package com.loopperfect.buckaroo;

import java.util.function.Function;

@Deprecated
public final class Functions {

    private Functions() {

    }

    @Deprecated
    public static <T> Function<? extends T, T> identity() {
        return x -> x;
    }
}
