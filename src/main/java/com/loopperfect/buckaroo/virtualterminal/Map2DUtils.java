package com.loopperfect.buckaroo.virtualterminal;

import com.google.common.base.Preconditions;

public final class Map2DUtils {

    private Map2DUtils() {

    }

    public static <T> Map2D<T> drawOn(final Map2D<T> a, final int x, final int y, final Map2D<T> b) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);
        return a.modify((i, j, v) -> {
            if (b.isInBounds(i - x, j - y)) {
                return b.get(i - x, j - y);
            }
            return v;
        });
    }

    public static Map2D<TerminalPixel> drawOnBackground(final Map2D<TerminalPixel> a, final int x, final int y, final Map2D<TerminalPixel> b) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);
        return a.modify((i, j, bg) -> {
            if (b.isInBounds(i - x, j - y)) {
                final TerminalPixel top = b.get(i - x, j - y);
                return TerminalPixel.of(top.character, top.foreground, top.background.isTransparent() ? bg.background : top.background  );
            }
            return bg;
        });
    }

    public static <T> Map2D<T> clip(final Map2D<T> map, final int width, final int height) {
        Preconditions.checkNotNull(map);
        Preconditions.checkArgument(width >= 0);
        Preconditions.checkArgument(height >= 0);
        if (map.width() <= width && map.height() <= height) {
            return map;
        }
        final int nextWidth = Math.min(map.width(), width);
        final int nextHeight = Math.min(map.height(), height);
        final T[][] nextValues = Arrays2D.create(map.valueType(), nextWidth, nextHeight);
        for (int x = 0; x < nextWidth; x++) {
            for (int y = 0; y < nextHeight; y++) {
                nextValues[x][y] = map.get(x, y);
            }
        }
        return Map2D.of(nextWidth, nextHeight, map.valueType(), nextValues);
    }

    public static <T> Map2D<T> pasteBelow(final Map2D<T> a, final Map2D<T> b) {

        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);

        Preconditions.checkArgument(a.width() == b.width());

        final T[][] values = Arrays2D.create(a.valueType(), a.width(), a.height() + b.height());

        for (int x = 0; x < a.width(); x++) {
            for (int y = 0; y < a.height(); y++) {
                values[x][y] = a.get(x, y);
            }
        }

        for (int x = 0; x < a.width(); x++) {
            for (int y = 0; y < b.height(); y++) {
                values[x][a.height() + y] = b.get(x, y);
            }
        }

        return Map2D.of(a.width(), a.height() + b.height(), a.valueType(), values);
    }

    public static <T> Map2D<T> pasteRight(final Map2D<T> a, final Map2D<T> b, final T background) {

        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);
        Preconditions.checkNotNull(background);

        int w = a.width() + b.width();
        int h = Math.max(a.height(), b.height());

        final T[][] values = Arrays2D.create(a.valueType(), w, h);

        Arrays2D.fill(values, background);

        for (int x = 0; x < a.width(); x++) {
            for (int y = 0; y < a.height(); y++) {
                values[x][y] = a.get(x, y);
            }
        }

        for (int x = 0; x < b.width(); x++) {
            for (int y = 0; y < b.height(); y++) {
                values[a.width() + x][y] = b.get(x, y);
            }
        }

        return Map2D.of(w, h, a.valueType(), values);
    }
}
