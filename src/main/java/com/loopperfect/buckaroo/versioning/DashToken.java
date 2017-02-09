package com.loopperfect.buckaroo.versioning;

public final class DashToken implements Token {

    private DashToken() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof DashToken;
    }

    public static DashToken of() {
        return new DashToken();
    }
}
