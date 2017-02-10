package com.loopperfect.buckaroo;

public final class Unit {

    private Unit() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj != null && obj instanceof Unit);
    }

    @Override
    public String toString() {
        return "()";
    }

    private static final Unit INSTANCE = new Unit();

    public static Unit of() {
        return INSTANCE;
    }
}
