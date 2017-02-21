package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

public final class ResolvedRecipe {

    public final RecipeIdentifier identifier;
    public final RecipeVersion recipeVersion;

    private ResolvedRecipe(final RecipeIdentifier identifier, final RecipeVersion recipeVersion) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.recipeVersion = Preconditions.checkNotNull(recipeVersion);
    }

    public static ResolvedRecipe of(final RecipeIdentifier identifier, final RecipeVersion recipeVersion) {
        return new ResolvedRecipe(identifier, recipeVersion);
    }
}
