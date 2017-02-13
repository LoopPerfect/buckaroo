package com.loopperfect.buckaroo;

import java.util.function.Function;

public final class Functions {

    private Functions() {

    }

    public static <T> Function<? extends T, T> identity() {
        return x -> x;
    }
}
