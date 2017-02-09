package com.loopperfect.buckaroo.versioning;

public final class OpenListToken implements Token {

    private OpenListToken() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof OpenListToken;
    }

    public static OpenListToken of() {
        return new OpenListToken();
    }
}
