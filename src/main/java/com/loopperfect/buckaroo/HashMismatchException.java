package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;

public final class HashMismatchException extends Exception {

    public final HashCode expected;
    public final HashCode actual;

    public HashMismatchException(final HashCode expected, final HashCode actual) {

        super();

        Preconditions.checkNotNull(expected);
        Preconditions.checkNotNull(actual);
        Preconditions.checkArgument(!expected.equals(actual));

        this.expected = expected;
        this.actual = actual;
    }

    @Override
    public String getMessage() {
        return "Hash mismatch! Expected " + expected + ", but got " + actual + ". ";
    }
}
