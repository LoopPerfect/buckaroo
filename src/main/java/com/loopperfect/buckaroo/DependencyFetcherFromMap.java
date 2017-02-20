package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by gaetano on 20/02/17.
 */
public final class DependencyFetcherFromMap implements DependencyFetcher {

    private final ImmutableMap<Identifier, ImmutableMap<SemanticVersion, DependencyGroup>> projects;

    private DependencyFetcherFromMap(final ImmutableMap<Identifier, ImmutableMap<SemanticVersion, DependencyGroup>> projects) {
        Preconditions.checkNotNull(projects);
        this.projects = projects;
    }

    @Override
    public Either<DependencyResolverException, ImmutableMap<SemanticVersion, DependencyGroup>> fetch(
            final Identifier id, final SemanticVersionRequirement versionRequirement) {

        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(versionRequirement);

        if (!projects.containsKey(id)) {
            return Either.left(new ProjectNotFoundException(id));
        }

        final ImmutableMap<SemanticVersion, DependencyGroup> candidates = projects.getOrDefault(id, ImmutableMap.of())
                .entrySet()
                .stream()
                .filter(entry -> versionRequirement.isSatisfiedBy(entry.getKey()))
                .collect(ImmutableMap.toImmutableMap(entry -> entry.getKey(), Map.Entry::getValue));

        if (candidates.isEmpty()) {
            return Either.left(new VersionRequirementNotSatisfiedException(id, versionRequirement));
        }

        return Either.right(candidates);
    }

    public static DependencyFetcher of(
            final ImmutableMap<Identifier, ImmutableMap<SemanticVersion, DependencyGroup>> projects) {
        return new DependencyFetcherFromMap(projects);
    }
}

