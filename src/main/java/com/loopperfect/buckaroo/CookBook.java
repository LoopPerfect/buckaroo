package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Objects;

public final class CookBook {

    public final ImmutableSet<Recipe> recipes;

    private CookBook(final ImmutableSet<Recipe> recipes) {
        this.recipes = Preconditions.checkNotNull(recipes);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof CookBook)) {
            return false;
        }
        final CookBook other = (CookBook) obj;
        return Objects.equals(recipes, other.recipes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipes);
    }

    public static CookBook of(final ImmutableSet<Recipe> recipes) {
        return new CookBook(recipes);
    }
}
