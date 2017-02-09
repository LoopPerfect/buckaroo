package com.loopperfect.buckaroo.versioning;

public final class AtMostToken implements Token {

    private AtMostToken() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof AtMostToken;
    }

    public static AtMostToken of() {
        return new AtMostToken();
    }
}
