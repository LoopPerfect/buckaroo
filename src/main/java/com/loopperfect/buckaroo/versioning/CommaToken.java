package com.loopperfect.buckaroo.versioning;

public final class CommaToken implements Token {

    private CommaToken() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof CommaToken;
    }

    public static CommaToken of() {
        return new CommaToken();
    }
}
