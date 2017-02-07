package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public final class RecipeBook {

    public final ImmutableSet<Recipe> recipes;

    public RecipeBook(final ImmutableSet<Recipe> recipes) {
        this.recipes = Preconditions.checkNotNull(recipes);
    }
}
