package com.loopperfect.buckaroo;

import io.reactivex.Single;

@FunctionalInterface
public interface RecipeSource {

    Single<Recipe> fetch(final RecipeIdentifier identifier);
}
