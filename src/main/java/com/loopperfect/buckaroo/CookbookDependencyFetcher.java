package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public final class CookbookDependencyFetcher implements DependencyFetcher {

    final ImmutableList<CookBook> cookBooks;

    private CookbookDependencyFetcher(final ImmutableList<CookBook> cookBooks) {
        this.cookBooks = Preconditions.checkNotNull(cookBooks);
    }

    @Override
    public Either<DependencyResolverException, ImmutableMap<SemanticVersion, DependencyGroup>> fetch(
            final Identifier project, final SemanticVersionRequirement versionRequirement) {

        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(versionRequirement);

        final ImmutableList<Recipe> recipes = cookBooks.stream()
                .flatMap(x -> x.recipes.stream())
                .filter(x -> x.name.equals(project))
                .collect(ImmutableList.toImmutableList());

        if (recipes.isEmpty()) {
            return Either.left(new ProjectNotFoundException(project));
        }

        return Either.right(recipes.stream()
                .flatMap(x -> x.versions.entrySet().stream())
                .filter(x -> versionRequirement.isSatisfiedBy(x.getKey()))
                .collect(ImmutableMap.toImmutableMap(
                        x -> x.getKey(),
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
