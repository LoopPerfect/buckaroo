package com.loopperfect.buckaroo.versioning;

public final class EqualsToken implements Token {

    private EqualsToken() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof EqualsToken;
    }

    public static EqualsToken of() {
        return new EqualsToken();
    }
}
