package com.loopperfect.buckaroo;

@FunctionalInterface
public interface ThrowingConsumer<T> {

    void accept(final T t) throws Throwable;
}
