package com.loopperfect.buckaroo;

import java.io.Serializable;
import java.util.Objects;

public final class Pair<T, U> implements Serializable {

    private static final long serialVersionUID = -3510502028021363658L;

    public final T a;
    public final U b;

    private Pair(final T a, final U b) {

        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        this.a = a;
        this.b = b;
    }

    public boolean equals(final Pair<T, U> other) {
        Objects.requireNonNull(other);
        return (this == other) || (
            Objects.equals(a, other.a) &&
            Objects.equals(b, other.b));
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && obj instanceof Pair) {
            final Pair<?, ?> other = (Pair<?, ?>) obj;
            return Objects.equals(a, other.a) &&
                Objects.equals(b, other.b);
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ")";
    }

    public static <T, U> Pair<T, U> of(final T a, final U b) {
        return new Pair<>(a, b);
    }
}
