package com.loopperfect.buckaroo.virtualterminal;

import com.google.common.base.Preconditions;

import java.lang.reflect.Array;
import java.util.Arrays;

public final class Arrays2D {

    private Arrays2D() {

    }

    @SuppressWarnings("unchecked")
    public static <T> T[][] create(final Class<T> componentType, final int width, final int height) {
        Preconditions.checkNotNull(componentType);
        Preconditions.checkArgument(width >= 0);
        Preconditions.checkArgument(height >= 0);
        return (T[][]) Array.newInstance(componentType, width, height);
    }

    public static <T> T[][] copy(final Class<T> componentType, final T[][] array) {
        final T[][] result = create(componentType, array.length, array.length > 0 ? array[0].length : 0);
        for (int i = 0; i < array.length; i++) {
            result[i] = Arrays.copyOf(array[i], array[i].length);
        }
        return result;
    }

    public static <T> void fill(final T[][] array, final T value) {
        Preconditions.checkNotNull(array);
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                array[i][j] = value;
            }
        }
    }
}
