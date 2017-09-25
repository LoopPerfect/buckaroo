package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public final class DependencyLocks {

    public final ImmutableList<RecipeIdentifier> projectDependencies;
    public final ImmutableList<ResolvedPlatformDependencies> platformDependencies;
    public final ImmutableMap<RecipeIdentifier, ResolvedDependency> locks;

    private DependencyLocks(
        final ImmutableList<RecipeIdentifier> projectDependencies,
        final ImmutableList<ResolvedPlatformDependencies> platformDependencies,
        final Map<RecipeIdentifier, ResolvedDependency> locks) {
        Preconditions.checkNotNull(projectDependencies);
        Preconditions.checkNotNull(platformDependencies);
        Preconditions.checkNotNull(locks);
        this.projectDependencies = projectDependencies;
        this.platformDependencies = platformDependencies;
        this.locks = ImmutableMap.copyOf(locks);
    }

    public boolean equals(final DependencyLocks other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(projectDependencies, other.projectDependencies) &&
            Objects.equals(platformDependencies, other.platformDependencies) &&
            Objects.equals(locks, other.locks);
    }

    public ImmutableList<DependencyLock> entries() {
        return locks.entrySet()
            .stream()
            .map(x -> DependencyLock.of(x.getKey(), x.getValue()))
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectDependencies, platformDependencies, locks);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof DependencyLocks &&
            equals((DependencyLocks)obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("locks", locks)
            .toString();
    }

    public static DependencyLocks of(final ResolvedDependencies resolvedDependencies) {

        Preconditions.checkNotNull(resolvedDependencies);
        Preconditions.checkArgument(resolvedDependencies.isComplete());

        final ImmutableList<DependencyLock> locks = resolvedDependencies.dependencies.entrySet()
            .stream()
            .map(entry -> {

                final RecipeVersion recipeVersion = entry.getValue().getValue1();

                final ImmutableList<RecipeIdentifier> xs =
                    recipeVersion.dependencies.map(x -> x.dependencies.entrySet()
                        .stream()
                        .map(Map.Entry::getKey)
                        .collect(ImmutableList.toImmutableList())).orElse(ImmutableList.of());

                return DependencyLock.of(
                    entry.getKey(),
                    ResolvedDependency.of(
                        recipeVersion.source,
                        recipeVersion.target,
                        recipeVersion.buckResource,
                        xs));
            })
            .collect(ImmutableList.toImmutableList());

        return DependencyLocks.of(locks);
    }

    public static DependencyLocks of(final Map<RecipeIdentifier, ResolvedDependency> locks) {
        return new DependencyLocks(ImmutableList.of(), ImmutableList.of(), locks);
    }

    public static DependencyLocks of(final Collection<DependencyLock> locks) {
        return new DependencyLocks(
            ImmutableList.of(),
            ImmutableList.of(),
            locks.stream()
                .collect(ImmutableMap.toImmutableMap(x -> x.identifier, x -> x.origin)));
    }

    public static DependencyLocks of(final DependencyLock... locks) {
        return DependencyLocks.of(ImmutableList.copyOf(locks));
    }

    public static DependencyLocks of() {
        return new DependencyLocks(ImmutableList.of(), ImmutableList.of(), ImmutableMap.of());
    }

    public static DependencyLocks of(
        final ImmutableList<RecipeIdentifier> projectDependencies,
        final ImmutableList<ResolvedPlatformDependencies> platformDependencies,
        final Map<RecipeIdentifier, ResolvedDependency> locks) {
        return new DependencyLocks(projectDependencies, platformDependencies, locks);
    }
}
