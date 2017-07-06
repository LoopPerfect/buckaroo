package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public final class DependencyLocks {

    public final ImmutableMap<RecipeIdentifier, ResolvedDependency> locks;

    private DependencyLocks(final Map<RecipeIdentifier, ResolvedDependency> locks) {
        Preconditions.checkNotNull(locks);
        this.locks = ImmutableMap.copyOf(locks);
    }

    public boolean equals(final DependencyLocks other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(locks, other.locks);
    }

    public ImmutableList<DependencyLock> entries() {
        return locks.entrySet()
            .stream()
            .map(x -> DependencyLock.of(x.getKey(), x.getValue()))
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(locks);
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

                final ImmutableList<ResolvedDependencyReference> xs =
                    recipeVersion.dependencies.map(x -> x.dependencies.entrySet()
                        .stream()
                        .map(i -> ResolvedDependencyReference.of(
                            i.getKey(),
                            resolvedDependencies.get(i.getKey()).getValue1().target))
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
        return new DependencyLocks(locks);
    }

    public static DependencyLocks of(final Collection<DependencyLock> locks) {
        return new DependencyLocks(locks.stream()
            .collect(ImmutableMap.toImmutableMap(x -> x.identifier, x -> x.origin)));
    }

    public static DependencyLocks of(final DependencyLock... locks) {
        return DependencyLocks.of(ImmutableList.copyOf(locks));
    }

    public static DependencyLocks of() {
        return new DependencyLocks(ImmutableMap.of());
    }
}
