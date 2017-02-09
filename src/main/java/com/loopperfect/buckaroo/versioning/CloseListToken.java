package com.loopperfect.buckaroo.versioning;

public final class CloseListToken implements Token {

    private CloseListToken() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof CloseListToken;
    }

    public static CloseListToken of() {
        return new CloseListToken();
    }
}
