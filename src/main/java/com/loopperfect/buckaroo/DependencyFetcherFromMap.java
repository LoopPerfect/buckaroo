package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by gaetano on 20/02/17.
 */
public class DependencyFetcherFromMap implements DependencyFetcher {
    private final ImmutableMap<Identifier, ImmutableMap<SemanticVersion,Project>> projects;

    DependencyFetcherFromMap(ImmutableMap<Identifier, ImmutableMap<SemanticVersion,Project>> projects) {
        this.projects = projects;
    }

    @Override
    public Either<
        DependencyResolverException,
        ImmutableMap<SemanticVersion, Project>> fetch(Identifier id, SemanticVersionRequirement req) {

        if (!projects.containsKey(id)) {
            return Either.left(
                new ProjectNotFoundException(id)
            );
        }

        final ImmutableMap<SemanticVersion, Project> candidates = ImmutableMap.copyOf(
            projects.getOrDefault(id, ImmutableMap.of())
                .entrySet()
                .stream()
                .filter(entry -> req.isSatisfiedBy(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

        if (candidates.isEmpty()) {
            return Either.left(
                new VersionRequirementNotSatisfiedException(id, req)
            );
        }

        return Either.right(candidates);
    }
}

