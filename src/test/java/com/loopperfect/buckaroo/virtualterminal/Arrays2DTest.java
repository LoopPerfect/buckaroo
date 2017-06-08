package com.loopperfect.buckaroo.virtualterminal;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class Arrays2DTest {

    @Test
    public void testCreate() {

        final Integer[][] array = Arrays2D.create(Integer.class, 2, 3);

        assertEquals(2, array.length);
        assertEquals(3, array[0].length);
    }

    @Test
    public void testCopy() {

        final Integer[][] array = Arrays2D.create(Integer.class, 2, 3);
        final Integer[][] copy = Arrays2D.copy(Integer.class, array);

        assertTrue(Arrays.deepEquals(array, copy));

        copy[1][2] = 123;

        assertFalse(Arrays.deepEquals(array, copy));
    }
}