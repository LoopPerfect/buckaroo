package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Since dependency information may exist remotely, we use this interface to lazily
 * resolve the dependency graph.
 * <p>
 * Given a dependency name and a version requirement, implementations should return a map
 * from available versions (that satisfy the requirement) to further dependencies that
 * may need to be resolved.
 */
@FunctionalInterface
public interface DependencyFetcher {

    Either<DependencyResolverException, ImmutableMap<SemanticVersion, DependencyGroup>> fetch(
        final RecipeIdentifier project, final SemanticVersionRequirement versionRequirement);

    default Either<DependencyResolverException, ImmutableMap<SemanticVersion, DependencyGroup>> fetch(
        final Dependency dependency) {
        Preconditions.checkNotNull(dependency);
        return fetch(dependency.project, dependency.versionRequirement);
    }
};
