package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class Dimensions {

    public final Optional<Integer> width;
    public final Optional<Integer> height;

    private Dimensions(final Optional<Integer> width, final Optional<Integer> height) {
        super();
        Preconditions.checkNotNull(width);
        Preconditions.checkNotNull(height);
        Preconditions.checkArgument(!width.isPresent() || width.get() >= 0);
        Preconditions.checkArgument(!height.isPresent() || height.get() >= 0);
        this.width = width;
        this.height = height;
    }

    public boolean equals(final Dimensions other) {
        Preconditions.checkNotNull(other);
        return this == other || (width.equals(other.width) && height.equals(other.height));
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof Dimensions && equals((Dimensions)obj));
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("width", width)
            .add("height", height)
            .toString();
    }

    public static Dimensions of(final int width, final int height) {
        return new Dimensions(Optional.of(width), Optional.of(height));
    }

    public static Dimensions of(final int width) {
        return new Dimensions(Optional.of(width), Optional.empty());
    }

    public static Dimensions of() {
        return new Dimensions(Optional.empty(), Optional.empty());
    }
}
