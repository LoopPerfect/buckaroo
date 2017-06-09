package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.Optional;

public final class MoreMaybes {

    private MoreMaybes() {

    }

    public static <T> Maybe<T> fromOptionalSingle(final Single<Optional<T>> x) {

        Preconditions.checkNotNull(x);

        return x.flatMapMaybe(i -> {

            if (i.isPresent()) {
                return Maybe.just(i.get());
            }

            return Maybe.empty();
        });
    }
}
