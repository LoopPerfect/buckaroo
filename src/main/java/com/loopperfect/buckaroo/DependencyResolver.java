package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;

public final class DependencyResolver {

    private DependencyResolver() {

    }

    private static Optional<Map.Entry<SemanticVersion, DependencyGroup>> getLatest(final ImmutableMap<SemanticVersion, DependencyGroup> versions) {
        Preconditions.checkNotNull(versions);
        return versions.entrySet()
            .stream()
            .max(Comparator.comparing(Map.Entry::getKey));
    }

    public static Either<ImmutableList<DependencyResolverException>, ImmutableMap<RecipeIdentifier, SemanticVersion>> resolve(
        final DependencyGroup dependencyGroup, final DependencyFetcher fetcher) {

        Preconditions.checkNotNull(dependencyGroup);
        Preconditions.checkNotNull(fetcher);

        final Stack<Dependency> todo = new Stack<>();
        final Map<RecipeIdentifier, SemanticVersion> resolved = new HashMap<>();
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


//    public static ImmutableSet<Identifier> removableDependencies(
//        final ImmutableMap<Identifier, SemanticVersionRequirement> dependencies,
//        final RecipeIdentifier project,
//        final DependencyFetcher fetcher) {
//
//        if (!dependencies.containsKey(project)) {
//            return ImmutableSet.of();
//        }
//
//        final SemanticVersionRequirement version = dependencies.get(project);
//        final ImmutableMap<SemanticVersion, DependencyGroup> removableVersions = fetcher.fetch(project, version)
//            .right()
//            .get();
//
//        return getLatest(removableVersions).map( removable ->
//            removable.getValue().dependencies
//                .keySet()
//                .stream()
//                .filter(x ->
//                    !dependencies.containsKey(x)
//                ).collect(ImmutableSet.toImmutableSet()))
//            .orElse(ImmutableSet.of());
//    }
}
