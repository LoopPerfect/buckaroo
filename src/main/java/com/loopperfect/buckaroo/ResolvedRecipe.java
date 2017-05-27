package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

public final class ResolvedRecipe {

    public final RecipeVersionIdentifier identifier;
    public final RecipeVersion version;

    private ResolvedRecipe(final RecipeVersionIdentifier identifier, final RecipeVersion version) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.version = Preconditions.checkNotNull(version);
    }

    public static ResolvedRecipe of(final RecipeVersionIdentifier identifier, final RecipeVersion recipeVersion) {
        return new ResolvedRecipe(identifier, recipeVersion);
    }
}
