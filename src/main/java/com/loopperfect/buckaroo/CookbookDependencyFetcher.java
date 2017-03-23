package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public final class CookbookDependencyFetcher implements DependencyFetcher {

    final ImmutableList<CookBook> cookBooks;

    private CookbookDependencyFetcher(final ImmutableList<CookBook> cookBooks) {
        this.cookBooks = Preconditions.checkNotNull(cookBooks);
    }

    @Override
    public Either<DependencyResolverException, ImmutableMap<SemanticVersion, DependencyGroup>> fetch(
        final RecipeIdentifier project, final SemanticVersionRequirement versionRequirement) {

        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(versionRequirement);

        final ImmutableList<Recipe> recipes = cookBooks.stream()
                .flatMap(x -> x.organizations
                        .entrySet()
                        .stream()
                        .flatMap(y -> y.getValue().recipes.entrySet()
                                .stream()
                                .filter(z -> RecipeIdentifier.of(y.getKey(), z.getKey()).equals(project))
                                .map(Map.Entry::getValue)))
                .collect(ImmutableList.toImmutableList());

        if (recipes.isEmpty()) {
            return Either.left(new ProjectNotFoundException(project));
        }

        return Either.right(recipes.stream()
            .flatMap(x -> x.versions.entrySet().stream())
            .filter(x -> versionRequirement.isSatisfiedBy(x.getKey()))
            .collect(ImmutableMap.toImmutableMap(
                    Map.Entry::getKey,
                    x -> x.getValue().dependencies)));
    }

    public static DependencyFetcher of(final ImmutableList<CookBook> cookBooks) {
        return new CookbookDependencyFetcher(cookBooks);
    }

    public static DependencyFetcher of(final CookBook cookBook) {
        Preconditions.checkNotNull(cookBook);
        return new CookbookDependencyFetcher(ImmutableList.of(cookBook));
    }
}
