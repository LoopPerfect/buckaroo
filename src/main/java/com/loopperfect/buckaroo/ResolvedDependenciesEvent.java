package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

public final class ResolvedDependenciesEvent implements Event {

    public final ImmutableMap<RecipeIdentifier, ResolvedDependency> dependencies;

    private ResolvedDependenciesEvent(final ImmutableMap<RecipeIdentifier, ResolvedDependency> dependencies) {
        Preconditions.checkNotNull(dependencies);
        this.dependencies = dependencies;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("dependencies", dependencies)
            .toString();
    }

    public static ResolvedDependenciesEvent of(final ImmutableMap<RecipeIdentifier, ResolvedDependency> dependencies) {
        return new ResolvedDependenciesEvent(dependencies);
    }

    @Deprecated
    public static ResolvedDependenciesEvent of2(final ImmutableMap<RecipeIdentifier, Pair<SemanticVersion, ResolvedDependency>> dependencies) {
        return new ResolvedDependenciesEvent(dependencies.entrySet()
            .stream()
            .map(x -> Maps.immutableEntry(x.getKey(), x.getValue().b))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
