package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public final class Size {

    public final int width;
    public final int height;

    private Size(final int width, final int height) {
        super();
        Preconditions.checkArgument(width >= 0);
        Preconditions.checkArgument(height >= 0);
        this.width = width;
        this.height = height;
    }

    public boolean equals(final Size other) {
        Preconditions.checkNotNull(other);
        return this == other || (width == other.width && height == other.height);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof Size && equals((Size)obj));
    }

    @Override
    public int hashCode() {
        return width ^ 3 * height ^ 7;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("width", width)
            .add("height", height)
            .toString();
    }

    public static Size of(final int width, final int height) {
        return new Size(width, height);
    }
}
