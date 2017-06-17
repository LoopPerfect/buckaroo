package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.stream.IntStream;

public final class CookbookDependencyFetcher implements DependencyFetcher {

    static final int CandidateThreshold = 3;
    final ImmutableList<CookBook> cookBooks;

    private CookbookDependencyFetcher(final ImmutableList<CookBook> cookBooks) {
        this.cookBooks = Preconditions.checkNotNull(cookBooks);
    }

    private static int calcRecipeDistance(final RecipeIdentifier cookbookEntry, final RecipeIdentifier project)
    {
        Preconditions.checkNotNull(cookbookEntry);
        Preconditions.checkNotNull(project);

        final String encodedCookbookEntry = cookbookEntry.encode();
        final String encodedProject = project.encode();
        final String minor = (encodedCookbookEntry.length() < encodedProject.length()) ? encodedCookbookEntry : encodedProject;
        final String major = (encodedCookbookEntry.length() < encodedProject.length()) ? encodedProject : encodedCookbookEntry;

        final int[] memo = IntStream.rangeClosed(0, minor.length()).toArray();
        for(char x : major.toCharArray())
        {
            int prev = memo[0];
            memo[0]++;
            for( int j = 1; j <= minor.length(); ++j)
            {
                final int substitution = prev + (x == minor.charAt(j-1) ? 0 : 1);
                prev = memo[j];
                memo[j] = Math.min(substitution, Math.min(memo[j-1], memo[j]) + 1);
            }
        }

        return memo[minor.length()];
    }

    @Override
    public Either<DependencyResolverException, ImmutableMap<SemanticVersion, DependencyGroup>> fetch(
        final RecipeIdentifier project, final SemanticVersionRequirement versionRequirement) {

        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(versionRequirement);

        final ImmutableList<Map.Entry<RecipeIdentifier,Recipe>> candidateRecipes = cookBooks.stream()
                .flatMap(x -> x.organizations
                        .entrySet()
                        .stream()
                        .flatMap(y -> y.getValue().recipes.entrySet()
                                .stream()
                                .map(z -> Maps.immutableEntry(RecipeIdentifier.of(y.getKey(), z.getKey()), z.getValue()))
                                .filter(z -> calcRecipeDistance(z.getKey(), project) < CandidateThreshold)))
                .collect(ImmutableList.toImmutableList());

        final ImmutableList<Recipe> recipes = candidateRecipes.stream()
                .filter(x -> x.getKey().equals(project))
                .map(x -> x.getValue())
                .collect(ImmutableList.toImmutableList());

        if (recipes.isEmpty()) {
            final ImmutableList<RecipeIdentifier> closeIds = candidateRecipes.stream()
                    .map(x -> x.getKey())
                    .collect(ImmutableList.toImmutableList());
            return Either.left(new ProjectNotFoundException(project, closeIds));
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
