package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import io.reactivex.Single;

import java.io.IOException;
import java.util.Map;

public final class CookbookRecipeSource implements RecipeSource {

    private final Cookbook cookbook;

    private CookbookRecipeSource(final Cookbook cookbook) {
        this.cookbook = Preconditions.checkNotNull(cookbook);
    }

    @Override
    public Process<Event, Recipe> fetch(final RecipeIdentifier identifier) {

        Preconditions.checkNotNull(identifier);

        return Process.of(Single.fromCallable(() -> {

            if (identifier.source.isPresent()) {
                throw new IllegalArgumentException(identifier.encode() + " should be found on " + identifier.source.get());
            }

            try {
                final Organization organization = MoreStreams.single(cookbook.organizations.entrySet()
                    .stream()
                    .filter(x -> x.getKey().equals(identifier.organization))
                    .map(Map.Entry::getValue));

                return MoreStreams.single(organization.recipes.entrySet()
                    .stream()
                    .filter(x -> x.getKey().equals(identifier.recipe))
                    .map(Map.Entry::getValue));
            } catch (final Throwable throwable) {
                throw new IOException("Could not find " + identifier.encode(), throwable);
            }
        }));
    }

    public static RecipeSource of(final Cookbook cookbook) {
        return new CookbookRecipeSource(cookbook);
    }
}
