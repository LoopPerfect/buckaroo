package com.loopperfect.buckaroo.virtualterminal;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.reactivex.functions.*;

import java.util.Arrays;
import java.util.Objects;

public final class Map2D<T> {

    private final int width;
    private final int height;

    private final Class<T> valueType;

    private final T[][] values;

    private Map2D(final int width, final int height, final Class<T> valueType, final T[][] values) {
        Preconditions.checkArgument(width >= 0);
        Preconditions.checkArgument(height >= 0);
        Preconditions.checkNotNull(valueType);
        Preconditions.checkNotNull(values);
        Preconditions.checkArgument(values.length == width);
        for (final T[] value : values) {
            Preconditions.checkArgument(value.length == height);
            for (final T i : value) {
                Preconditions.checkNotNull(i);
            }
        }
        this.width = width;
        this.height = height;
        this.valueType = valueType;
        this.values = values;
    }

    public Class<T> valueType() {
        return valueType;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public T get(final int x, final int y) {
        Preconditions.checkArgument(isInBounds(x, y));
        return values[x][y];
    }

    public boolean isInBounds(final int x, final int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean equals(final Map2D<T> other) {
        Preconditions.checkNotNull(other);
        return (this == other) ||
            (width == other.width &&
                height == other.height &&
                valueType.equals(other.valueType) &&
                Arrays.deepEquals(values, other.values));
    }

    public Map2D<T> set(final int x, final int y, final T newValue) {
        Preconditions.checkArgument(isInBounds(x, y));
        Preconditions.checkNotNull(newValue);
        if (get(x, y).equals(newValue)) {
            return this;
        }
        final T[][] nextValues = Arrays2D.copy(valueType, values);
        nextValues[x][y] = newValue;
        return Map2D.of(width, height, valueType, nextValues);
    }

    public Map2D<T> modify(final Function3<Integer, Integer, T, T> f) {
        Preconditions.checkNotNull(f);
        final T[][] nextValues = Arrays2D.create(valueType, width, height);
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    nextValues[x][y] = Preconditions.checkNotNull(f.apply(x, y, get(x, y)));
                }
            }
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
        return Map2D.of(width, height, valueType, nextValues);
    }

    public Map2D<T> write(final int x, final int y, final ImmutableList<T> xs) {
        Preconditions.checkArgument(isInBounds(x, y));
        Preconditions.checkNotNull(xs);
        if (xs.isEmpty()) {
            return this;
        }
        final T[][] nextValues = Arrays2D.copy(valueType, values);
        for (int i = 0; i < xs.size(); i++) {
            if (x + i >= width) {
                break;
            }
            nextValues[x + i][y] = xs.get(i);
        }
        return Map2D.of(width, height, valueType, nextValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, values);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof Map2D &&
            equals((Map2D) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("width", width)
            .add("height", height)
            .add("values", values)
            .toString();
    }

    public static <T> Map2D<T>of(final int width, final int height, final Class<T> valueType, final T value) {
        Preconditions.checkArgument(width >= 0);
        Preconditions.checkArgument(height >= 0);
        Preconditions.checkNotNull(value);
        final T[][] values = Arrays2D.create(valueType, width, height);
        for (int column = 0; column < width; column++) {
            for (int row = 0; row < height; row++) {
                values[column][row] = value;
            }
        }
        return new Map2D<T>(width, height, valueType, values);
    }

    public static <T> Map2D<T>of(final int width, final int height, final Class<T> valueType, final T[][] values) {
        return new Map2D<T>(width, height, valueType, values);
    }
}
