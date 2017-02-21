package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class DependencyGroup {

    public final ImmutableMap<Identifier, SemanticVersionRequirement> dependencies;

    private DependencyGroup(final ImmutableMap<Identifier, SemanticVersionRequirement> dependencies) {
        this.dependencies = Preconditions.checkNotNull(dependencies);
    }

    public boolean isEmpty() {
        return dependencies.isEmpty();
    }

    public boolean requires(final Identifier identifier) {
        Preconditions.checkNotNull(identifier);
        return dependencies.containsKey(identifier);
    }

    public ImmutableList<Dependency> entries() {
        return dependencies.entrySet()
                .stream()
                .map(x -> Dependency.of(x.getKey(), x.getValue()))
                .collect(ImmutableList.toImmutableList());
    }

    public DependencyGroup addDependency(final Dependency dependency) {
        Preconditions.checkNotNull(dependency);
        if (dependencies.containsKey(dependency.project) &&
                dependencies.get(dependency.project).equals(dependency.versionRequirement)) {
            return this;
        }
        return new DependencyGroup(
                Stream.concat(
                        dependencies.entrySet()
                                .stream()
                                .filter(x -> !x.getKey().equals(dependency.project)),
                        Stream.of(Maps.immutableEntry(dependency.project, dependency.versionRequirement)))
                        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public DependencyGroup removeDependency(final Identifier identifier) {
        Preconditions.checkNotNull(identifier);
        if (!dependencies.containsKey(identifier)) {
            return this;
        }
        return new DependencyGroup(dependencies.entrySet()
                .stream()
                .filter(x -> !x.getKey().equals(identifier))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public boolean isSatisfiedBy(final ImmutableSet<RecipeIdentifier> recipes) {
        Preconditions.checkNotNull(recipes);
        return dependencies.entrySet()
                .stream()
                .allMatch(x -> recipes.stream()
                        .anyMatch(y -> x.getKey().equals(y.project) &&
                                x.getValue().isSatisfiedBy(y.version)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencies);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof DependencyGroup)) {
            return false;
        }
        final DependencyGroup other = (DependencyGroup) obj;
        return Objects.equals(dependencies, other.dependencies);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("dependencies", dependencies)
                .toString();
    }

    public static DependencyGroup of(final ImmutableMap<Identifier, SemanticVersionRequirement> dependencies) {
        return new DependencyGroup(dependencies);
    }

    public static DependencyGroup of() {
        return new DependencyGroup(ImmutableMap.of());
    }
}
