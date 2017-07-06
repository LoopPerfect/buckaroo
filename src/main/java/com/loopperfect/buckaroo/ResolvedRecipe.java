package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

public final class ResolvedRecipe {

    public final RecipeVersionIdentifier identifier;
    public final ResolvedDependency version;

    private ResolvedRecipe(final RecipeVersionIdentifier identifier, final ResolvedDependency version) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.version = Preconditions.checkNotNull(version);
    }

    public static ResolvedRecipe of(final RecipeVersionIdentifier identifier, final ResolvedDependency recipeVersion) {
        return new ResolvedRecipe(identifier, recipeVersion);
    }
}
