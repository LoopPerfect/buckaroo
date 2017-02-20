package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.io.IOException;

public final class Try {

    private Try() {

    }

    public static <R, E extends Throwable> Either<E, R> safe(final CheckedSupplier<R, E> f, final Class<E> exceptionType) {
        Preconditions.checkNotNull(f);
        Preconditions.checkNotNull(exceptionType);
        try {
            return Either.right(f.get());
        } catch (final Throwable e) {
            if (exceptionType.isInstance(e)) {
                return Either.left(exceptionType.cast(e));
            }
            throw new IllegalStateException("A CheckedSupplier may only throw its specified Throwable type");
        }
    }

    public static <R> Either<IOException, R> safe(final CheckedSupplier<R, IOException> f) {
        return safe(f, IOException.class);
    }
}
