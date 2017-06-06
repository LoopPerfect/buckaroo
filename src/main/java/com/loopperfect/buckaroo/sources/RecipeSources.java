package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.resolver.DependencyResolutionException;
import com.loopperfect.buckaroo.versioning.ExactSemanticVersion;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Comparator;

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

    public static RecipeSource standard(final FileSystem fs, final BuckarooConfig config) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(config);

        final Path cookbookPath = fs.getPath(
            System.getProperty("user.home"),
            ".buckaroo",
            config.cookbooks.get(0).name.name);

        return RecipeSources.routed(
            ImmutableMap.of(
                Identifier.of("github"), GitHubRecipeSource.of()),
            LazyCookbookRecipeSource.of(cookbookPath));
    }

    public static Observable<Dependency> resolve(final RecipeSource source, final PartialDependency dependency) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(dependency);

        return source.fetch(RecipeIdentifier.of(dependency.organization, dependency.project))
            .map(x -> Dependency.of(
                RecipeIdentifier.of(dependency.source, dependency.organization, dependency.project),
                ExactSemanticVersion.of(x.versions.keySet().stream().max(Comparator.naturalOrder())
                    .orElseThrow(() -> new IOException(dependency.encode() + " has no versions! ")))))
            .toObservable();
    }
}
