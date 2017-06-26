package com.loopperfect.buckaroo.virtualterminal;

import com.google.common.base.Preconditions;

public final class Map2DBuilder<T> {

    private final Class<T> valueType;

    private int width;
    private int height;
    private T[][] values;

    public Map2DBuilder(int width, int height, final Class<T> valueType, final T value) {

        Preconditions.checkArgument(width > 0);
        Preconditions.checkArgument(height > 0);

        this.valueType = Preconditions.checkNotNull(valueType);

        this.width = width;
        this.height = height;
        this.values = Arrays2D.create(valueType, width, height);

        for (int column = 0; column < width; column++) {
            for (int row = 0; row < height; row++) {
                this.values[column][row] = value;
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Map2DBuilder<T> setWidth(final int nextWidth) {
        Preconditions.checkArgument(nextWidth >= 0);
        if (this.width != nextWidth) {
            final T[][] nextValues = Arrays2D.create(valueType, nextWidth, height);
            for (int x = 0; x < nextWidth; x++) {
                System.arraycopy(values[x], 0, nextValues[x], 0, height);
            }
            this.width = nextWidth;
        }
        return this;
    }

    public Map2DBuilder<T> setHeight(final int nextHeight) {
        Preconditions.checkArgument(nextHeight >= 0);
        if (this.height != nextHeight) {
            final T[][] nextValues = Arrays2D.create(valueType, width, nextHeight);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < nextHeight; y++) {
                    nextValues[x][y] = values[x][y];
                }
            }
            this.height = nextHeight;
        }
        return this;
    }

    public Map2DBuilder<T> set(final int x, final int y, final T value) {
        Preconditions.checkArgument(isInBounds(x, y));
        this.values[x][y] = value;
        return this;
    }

    public T value(final int x, final int y) {
        Preconditions.checkArgument(isInBounds(x, y));
        return values[x][y];
    }

    public boolean isInBounds(final int x, final int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public Map2D<T> build() {
        return Map2D.of(width, height, valueType, values);
    }
}
