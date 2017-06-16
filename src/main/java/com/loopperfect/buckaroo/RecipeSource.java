package com.loopperfect.buckaroo;

@FunctionalInterface
public interface RecipeSource {

    Process<Event, Recipe> fetch(final RecipeIdentifier identifier);
}
