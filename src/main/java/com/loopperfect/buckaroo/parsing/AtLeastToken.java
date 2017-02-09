package com.loopperfect.buckaroo.parsing;

public final class AtLeastToken implements Token {

    private AtLeastToken() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof AtLeastToken;
    }

    public static AtLeastToken of() {
        return new AtLeastToken();
    }
}
