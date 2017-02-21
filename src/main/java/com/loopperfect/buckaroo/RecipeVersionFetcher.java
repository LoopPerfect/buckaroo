package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Optional;

@FunctionalInterface
public interface RecipeVersionFetcher {

    Optional<RecipeVersion> fetch(final RecipeIdentifier identifier);

    static RecipeVersionFetcher of(final ImmutableList<CookBook> cookBooks) {
        Preconditions.checkNotNull(cookBooks);
        return identifier -> cookBooks.stream()
                .flatMap(x -> x.recipes.stream())
                .filter(x -> x.name.equals(identifier.project))
                .flatMap(x -> x.versions.entrySet().stream()
                        .filter(y -> y.getKey().equals(identifier.version)))
                .map(x -> x.getValue())
                .findAny();
    }

    static RecipeVersionFetcher of(final CookBook cookBook) {
        Preconditions.checkNotNull(cookBook);
        return of(ImmutableList.of(cookBook));
    }
}
