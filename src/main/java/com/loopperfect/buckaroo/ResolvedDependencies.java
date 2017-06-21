package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.javatuples.Pair;

import java.util.Map;
import java.util.Objects;

public final class ResolvedDependencies {

    public final ImmutableMap<RecipeIdentifier, Pair<SemanticVersion, RecipeVersion>> dependencies;

    private ResolvedDependencies(final Map<RecipeIdentifier, Pair<SemanticVersion, RecipeVersion>> dependencies) {
        Preconditions.checkNotNull(dependencies);
        this.dependencies = ImmutableMap.copyOf(dependencies);
    }

    public ResolvedDependencies add(final RecipeIdentifier identifier, final Pair<SemanticVersion, RecipeVersion> dependency) {
        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(dependency);
        return ResolvedDependencies.of(
            MoreMaps.with(
                dependencies,
                identifier, dependency));
    }

    public boolean equals(final ResolvedDependencies other) {
        Preconditions.checkNotNull(other);
        return other == this ||
            Objects.equals(dependencies, other.dependencies);
    }

    public Pair<SemanticVersion, RecipeVersion> get(final RecipeIdentifier identifier) {
        Preconditions.checkNotNull(identifier);
        return dependencies.get(identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencies);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this ||
            obj != null &&
                obj instanceof ResolvedDependencies &&
                equals((ResolvedDependencies) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("dependencies", dependencies)
            .toString();
    }

    public static ResolvedDependencies of(final Map<RecipeIdentifier, Pair<SemanticVersion, RecipeVersion>> dependencies) {
        return new ResolvedDependencies(dependencies);
    }

    public static ResolvedDependencies of() {
        return new ResolvedDependencies(ImmutableMap.of());
    }
}
