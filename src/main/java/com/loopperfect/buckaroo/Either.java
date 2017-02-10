package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class Either<L, R> {

    private enum LeftOrRight {
        LEFT,
        RIGHT
    }

    private final LeftOrRight which;

    // l or r will be null depending on the value of which
    private final L l;
    private final R r;

    private Either(final LeftOrRight which, final L l, final R r) {
        // We do not check l and r are not null here; this is guaranteed by the factory.
        this.which = which;
        this.l = l;
        this.r = r;
    }

    public <T, U> Either<T, U> flatMap(final Function<L, T> f, final Function<R, U> g) {
        Preconditions.checkNotNull(f);
        Preconditions.checkNotNull(g);
        return which == LeftOrRight.LEFT ?
                Either.left(f.apply(l)) :
                Either.right(g.apply(r));
    }

    public <T> Either<T, R> leftProjection(final Function<L, T> f) {
        Preconditions.checkNotNull(f);
        return which == LeftOrRight.LEFT ?
                Either.left(f.apply(l)) :
                Either.right(r);
    }

    public <T> Either<L, T> rightProjection(final Function<R, T> f) {
        Preconditions.checkNotNull(f);
        return which == LeftOrRight.LEFT ?
                Either.left(l) :
                Either.right(f.apply(r));
    }

    public <T> T join(final Function<L, T> f, final Function<R, T> g) {
        Preconditions.checkNotNull(f);
        Preconditions.checkNotNull(g);
        return which == LeftOrRight.LEFT ?
                f.apply(l) :
                g.apply(r);
    }

    public Optional<R> toOptional() {
        return which == LeftOrRight.LEFT ? Optional.empty() : Optional.of(r);
    }

    @Override
    public int hashCode() {
        return which == LeftOrRight.LEFT ? Objects.hash(l) : Objects.hash(r);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof Either)) {
            return false;
        }

        final Either<L, R> other = (Either<L, R>)obj;

        return Objects.equals(which, other.which) &&
                (which == LeftOrRight.LEFT ?
                        Objects.equals(l, other.l) :
                        Objects.equals(r, other.r));
    }

    @Override
    public String toString() {
        return which == LeftOrRight.LEFT ?
                "[Left " + l + "]" :
                "[Right " + r + "]";
    }

    public static <L, R> Either<L, R> left(final L x) {
        Preconditions.checkNotNull(x);
        return new Either<>(LeftOrRight.LEFT, x, null);
    }

    public static <L, R> Either<L, R> right(final R x) {
        Preconditions.checkNotNull(x);
        return new Either<>(LeftOrRight.RIGHT, null, x);
    }
}
