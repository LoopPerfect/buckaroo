package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Objects;

public final class BuckarooConfig {

    public final ImmutableList<RemoteCookBook> cookBooks;

    private BuckarooConfig(final ImmutableList<RemoteCookBook> cookBooks) {
        this.cookBooks = Preconditions.checkNotNull(cookBooks);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof BuckarooConfig)) {
            return false;
        }
        final BuckarooConfig other = (BuckarooConfig) obj;
        return Objects.equals(cookBooks, other.cookBooks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cookBooks);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("cookBooks", cookBooks)
            .toString();
    }

    public static BuckarooConfig of(final ImmutableList<RemoteCookBook> cookBooks) {
        return new BuckarooConfig(cookBooks);
    }
}
