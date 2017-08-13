package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.IOException;

@FunctionalInterface
public interface RecipeSource {

    Process<Event, Recipe> fetch(final RecipeIdentifier identifier);

    default Iterable<RecipeIdentifier> findCandidates(final PartialDependency partial) {
        Preconditions.checkNotNull(partial);
        return findCandidates(PartialRecipeIdentifier.of(
            partial.source,
            partial.organization,
            partial.project));
    }

    default Iterable<RecipeIdentifier> findCandidates(final PartialRecipeIdentifier partial) {
        return ImmutableList.of();
    }

    default Iterable<RecipeIdentifier> findSimilar(final RecipeIdentifier identifier) {
        return ImmutableList.of();
    }
}
