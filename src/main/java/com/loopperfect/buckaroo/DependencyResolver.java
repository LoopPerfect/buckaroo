package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.*;

/**
 * Utility for resolving dependencies and transient dependencies.
 */
public final class DependencyResolver {

    private DependencyResolver() {

    }

    private static Optional<Map.Entry<SemanticVersion, DependencyGroup>> getLatest(final ImmutableMap<SemanticVersion, DependencyGroup> versions) {
        Preconditions.checkNotNull(versions);
        return versions.entrySet()
            .stream()
            .max(Comparator.comparing(Map.Entry::getKey));
    }

    public static Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>> resolve(
        final DependencyGroup dependencyGroup, final DependencyFetcher fetcher) {

        Preconditions.checkNotNull(dependencyGroup);
        Preconditions.checkNotNull(fetcher);

        final Stack<Dependency> todo = new Stack<>();
        final Map<Identifier, SemanticVersion> resolved = new HashMap<>();
        final List<DependencyResolverException> unresolved = new ArrayList<>();

        todo.addAll(dependencyGroup.entries());

        while (!todo.isEmpty()) {

            final Dependency next = todo.pop();

            if (resolved.containsKey(next.project)) {
                if (!next.versionRequirement.isSatisfiedBy(resolved.get(next.project))) {
                    unresolved.add(new VersionRequirementNotSatisfiedException(next.project, next.versionRequirement));
                }
                continue;
            }

            fetcher.fetch(next).join(
                error -> Action.of(() -> unresolved.add(error)),
                x -> Action.of(() -> {
                    final Optional<Map.Entry<SemanticVersion, DependencyGroup>> latest = getLatest(x);
                    if (latest.isPresent()) {
                        resolved.put(next.project, latest.get().getKey());
                        todo.addAll(latest.get().getValue().entries());
                    } else {
                        unresolved.add(new VersionRequirementNotSatisfiedException(
                            next.project, next.versionRequirement));
                    }
                })).run();
        }

        if (unresolved.isEmpty()) {
            return Either.right(ImmutableMap.copyOf(resolved));
        }

        return Either.left(ImmutableList.copyOf(unresolved));
    }
}
