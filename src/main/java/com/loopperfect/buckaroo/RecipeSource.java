package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;

@FunctionalInterface
public interface RecipeSource {
    Process<Event, Recipe> fetch(final RecipeIdentifier identifier);

    public default Iterable<RecipeIdentifier> findCandidates(final RecipeIdentifier identifier) {
        return ImmutableList.of();
    }
}
