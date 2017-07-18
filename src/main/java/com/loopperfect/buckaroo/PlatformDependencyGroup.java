package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.javatuples.Pair;

import java.util.List;
import java.util.Objects;

public final class PlatformDependencyGroup {

    public final List<Pair<String, DependencyGroup>> platformDependencies;

    private PlatformDependencyGroup(final List<Pair<String, DependencyGroup>> platformDependencies) {
        Preconditions.checkNotNull(platformDependencies);
        Preconditions.checkArgument(platformDependencies.stream()
            .noneMatch(x -> x == null || x.getValue0() == null || x.getValue1() == null));
        this.platformDependencies = ImmutableList.copyOf(platformDependencies);
    }

    public Iterable<Pair<String, DependencyGroup>> entries() {
        return platformDependencies;
    }

    public boolean any() {
        return !platformDependencies.isEmpty();
    }

    public List<String> platforms() {
        return platformDependencies.stream()
            .map(Pair::getValue0)
            .distinct()
            .collect(ImmutableList.toImmutableList());
    }

    public DependencyGroup allPlatforms() {
        return platformDependencies.stream()
            .reduce(
                DependencyGroup.of(),
                (DependencyGroup x, Pair<String, DependencyGroup> y) -> x.add(y.getValue1().entries()),
                (x, y) -> x.add(y.entries()));
    }

    public boolean equals(final PlatformDependencyGroup other) {
        Preconditions.checkNotNull(other);
        return this == other ||
            Objects.equals(platformDependencies, other.platformDependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platformDependencies);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null &&
                obj instanceof PlatformDependencyGroup &&
                equals((PlatformDependencyGroup)obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .addValue(platformDependencies)
            .toString();
    }

    public static PlatformDependencyGroup of(final List<Pair<String, DependencyGroup>> platformDependencies) {
        return new PlatformDependencyGroup(platformDependencies);
    }

    public static PlatformDependencyGroup of() {
        return new PlatformDependencyGroup(ImmutableList.of());
    }
}
