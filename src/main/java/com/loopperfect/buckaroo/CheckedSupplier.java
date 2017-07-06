package com.loopperfect.buckaroo;

@FunctionalInterface
public interface CheckedSupplier<R, E extends Throwable> {

    R get() throws E;
}
