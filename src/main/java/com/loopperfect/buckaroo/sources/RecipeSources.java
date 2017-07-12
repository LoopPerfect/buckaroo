package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.github.GitHubRecipeSource;
import com.loopperfect.buckaroo.resolver.DependencyResolutionException;
import com.loopperfect.buckaroo.versioning.ExactSemanticVersion;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Comparator;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class RecipeSources {

    private RecipeSources() {

    }

    public static RecipeSource empty() {
        return identifier ->
            Process.error(new IOException(identifier.encode() + " not found. This source is empty. "));
    }

    public static RecipeSource routed(final ImmutableMap<Identifier, RecipeSource> routes, final RecipeSource otherwise) {
        Preconditions.checkNotNull(routes);
        Preconditions.checkNotNull(otherwise);
        return new RecipeSource() {

            public Process<Event, Recipe> fetch (final RecipeIdentifier identifier) {
                if (identifier.source.isPresent()) {
                    if (routes.containsKey(identifier.source.get())) {
                        return routes.get(identifier.source.get()).fetch(identifier);
                    }
                    return Process.error(new DependencyResolutionException(
                        "Could not fetch " + identifier.encode() + " because " + identifier.source.get() + " is not routed. "));
                }
                return otherwise.fetch(identifier);
            }

            public Iterable<RecipeIdentifier> findCandidates(PartialRecipeIdentifier partial) {
                if (partial.source.isPresent()) {
                    if (routes.containsKey(partial.source.get())) {
                        return routes.get(partial.source.get()).findCandidates(partial);
                    }
                }
                return otherwise.findCandidates(partial);
            }
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
                Identifier.of("github"), GitHubRecipeSource.of(fs)),
            LazyCookbookRecipeSource.of(cookbookPath));
    }

    public static Process<Event, RecipeIdentifier> selectDependency(final RecipeSource source, final PartialDependency dependency) {
        if (dependency.organization.isPresent()) {
            return Process.of(
                Single.just(RecipeIdentifier.of(dependency.source, dependency.organization.get(), dependency.project))
            );
        }
        final ImmutableList<RecipeIdentifier> candidates = Streams.stream(source
            .findCandidates(dependency))
            .limit(5)
            .collect(toImmutableList());

        if (candidates.size() == 0) {
            return Process.error(
                PartialDependencyResolutionException.of(candidates, dependency));
        }

        if (candidates.size() > 1) {
            return Process.error(PartialDependencyResolutionException.of(candidates, dependency));
        }

        return Process.of(
            Observable.just(
                Notification.of("resolved partial dependency: " + dependency.toString()+ " to "+ candidates.get(0).toString())),
            Single.just(candidates.get(0)));
    }

    public static Process<Event, Dependency> resolve(final RecipeSource source, final PartialDependency dependency) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(dependency);

        return selectDependency(source, dependency).chain(selected ->
            source.fetch(RecipeIdentifier.of(selected.source, selected.organization, selected.recipe))
                .chain(recipe -> Process.of(Single.just(recipe).map(x -> Dependency.of(
                    RecipeIdentifier.of(dependency.source, selected.organization, selected.recipe),
                        ExactSemanticVersion.of(x.versions.keySet().stream()
                        .max(Comparator.naturalOrder())
                        .orElseThrow(() -> new IOException(dependency.encode() + " has no versions! "))))))));
    }
}
