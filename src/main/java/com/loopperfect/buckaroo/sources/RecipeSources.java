package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.DependencyResolutionException;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RecipeSource;
import io.reactivex.Single;

import java.io.IOException;

public final class RecipeSources {

    private RecipeSources() {

    }

    public static RecipeSource empty() {
        return identifier ->
            Single.error(new IOException(identifier.encode() + " not found. This source is empty. "));
    }

    public static RecipeSource routed(final ImmutableMap<Identifier, RecipeSource> routes, final RecipeSource otherwise) {
        Preconditions.checkNotNull(routes);
        Preconditions.checkNotNull(otherwise);
        return identifier -> {
            if (identifier.source.isPresent()) {
                if (routes.containsKey(identifier.source.get())) {
                    return routes.get(identifier.source.get()).fetch(identifier);
                }
                return Single.error(new DependencyResolutionException(
                    "Could not fetch " + identifier.encode() + " because " + identifier.source.get() + " is not routed. "));
            }
            return otherwise.fetch(identifier);
        };
    }
}
