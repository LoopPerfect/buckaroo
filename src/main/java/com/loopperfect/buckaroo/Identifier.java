package com.loopperfect.buckaroo;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public final class Identifier {

    public final String name;

    private Identifier(final String name) {

        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(isValid(name));

        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null || !(obj instanceof Identifier)) {
            return false;
        }

        Identifier other = (Identifier) obj;

        return Objects.equal(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }

    public static Identifier of(final String name) {
        return new Identifier(name);
    }

    public static boolean isValid(final String x) {
        Preconditions.checkNotNull(x);
        return x.length() > 2 && x.length() < 30 && x.matches("^[a-zA-Z0-9-_]*$");
    }
}
