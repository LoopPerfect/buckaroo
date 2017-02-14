package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Optionals {

    private Optionals() {

    }

    public static <T, U> U join(final Optional<? extends T> optional, final Function<T, ? extends U> f, final Supplier<? extends U> g) {
        Preconditions.checkNotNull(optional);
        Preconditions.checkNotNull(f);
        Preconditions.checkNotNull(g);
        if (optional.isPresent()) {
            return f.apply(optional.get());
        }
        return g.get();
    }
}
