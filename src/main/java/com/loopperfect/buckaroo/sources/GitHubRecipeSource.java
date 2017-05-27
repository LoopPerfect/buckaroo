package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.github.GitHub;
import io.reactivex.Single;

import java.io.IOException;

public final class GitHubRecipeSource implements RecipeSource {

    private GitHubRecipeSource() {

    }

    @Override
    public Single<Recipe> fetch(final RecipeIdentifier identifier) {
        Preconditions.checkNotNull(identifier);
        return GitHub.fetchReleaseNames(identifier.organization, identifier.recipe)
            .flatMap(ignored ->
                Single.error(new IOException("The GitHub dependency source is not implemented yet. ")));
    }

    public static RecipeSource of() {
        return new GitHubRecipeSource();
    }
}
