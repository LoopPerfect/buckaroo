package com.loopperfect.buckaroo.virtualterminal;

@FunctionalInterface
public interface Function3<X, Y, Z, W> {

    W apply(final X a, final Y b, final Z c);
}
