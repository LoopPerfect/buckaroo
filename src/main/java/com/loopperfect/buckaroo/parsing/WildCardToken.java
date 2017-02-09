package com.loopperfect.buckaroo.parsing;

public final class WildCardToken implements Token {

    private WildCardToken() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof WildCardToken;
    }

    public static WildCardToken of() {
        return new WildCardToken();
    }
}
