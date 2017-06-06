package com.loopperfect.buckaroo;

import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public final class EitherTest {

    private static int square(final int x) {
        return x * x;
    }

    private static String twice(final String s) {
        return s + s;
    }

    @Test
    public void liftLeft() throws Exception {

        final Function<Either<Integer, String>, Either<Integer, String>> f = Either.liftLeft(EitherTest::square);

        assertEquals(Either.left(123 * 123), f.apply(Either.left(123)));
        assertEquals(Either.right("abc"), f.apply(Either.right("abc")));
    }

    @Test
    public void liftRight() throws Exception {

        final Function<Either<Integer, String>, Either<Integer, String>> f = Either.liftRight(EitherTest::twice);

        assertEquals(Either.left(123), f.apply(Either.left(123)));
        assertEquals(Either.right("abcabc"), f.apply(Either.right("abc")));
    }
}
