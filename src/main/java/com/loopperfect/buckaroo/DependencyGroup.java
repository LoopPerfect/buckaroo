package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Maps.immutableEntry;

public final class DependencyGroup {

    public final ImmutableMap<RecipeIdentifier, SemanticVersionRequirement> dependencies;

    private DependencyGroup(final ImmutableMap<RecipeIdentifier, SemanticVersionRequirement> dependencies) {
        this.dependencies = Preconditions.checkNotNull(dependencies);
    }

    public boolean any() {
        return !dependencies.isEmpty();
    }

    public boolean requires(final RecipeIdentifier identifier) {
        Preconditions.checkNotNull(identifier);
        return dependencies.containsKey(identifier);
    }

    public ImmutableList<Dependency> entries() {
        return dependencies.entrySet()
            .stream()
            .map(x -> Dependency.of(x.getKey(), x.getValue()))
            .collect(ImmutableList.toImmutableList());
    }

    public DependencyGroup add(final Dependency dependency) {
        Preconditions.checkNotNull(dependency);
        if (dependencies.containsKey(dependency.project) &&
            dependencies.get(dependency.project).equals(dependency.requirement)) {
            return this;
        }
        return new DependencyGroup(
            Stream.concat(
                dependencies.entrySet()
                    .stream()
                    .filter(x -> !x.getKey().equals(dependency.project)),
                Stream.of(immutableEntry(dependency.project, dependency.requirement)))
                .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public DependencyGroup add(final Collection<Dependency> newDependencies) {
        Preconditions.checkNotNull(newDependencies);
        if (newDependencies.stream().allMatch(x ->
            this.dependencies.containsKey(x.project) &&
                this.dependencies.get(x.project).equals(x.requirement))) {
            return this;
        }
        return new DependencyGroup(
            Stream.concat(
                dependencies.entrySet()
                    .stream()
                    .filter(x -> newDependencies.stream().noneMatch(y -> x.getKey().equals(y.project))),
                newDependencies.stream()
                    .distinct()
                    .map(x -> immutableEntry(x.project, x.requirement))
            ).collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public DependencyGroup remove(final RecipeIdentifier identifier) {
        Preconditions.checkNotNull(identifier);
        if (!dependencies.containsKey(identifier)) {
            return this;
        }
        return new DependencyGroup(dependencies.entrySet()
            .stream()
            .filter(x -> !x.getKey().equals(identifier))
            .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public DependencyGroup remove(final Collection<Dependency> toRemove) {
        Preconditions.checkNotNull(toRemove);
        if (toRemove.stream().noneMatch(x ->
            this.dependencies.containsKey(x.project) &&
                this.dependencies.get(x.project).equals(x.requirement))) {
            return this;
        }
        return new DependencyGroup(
            dependencies.entrySet()
                .stream()
                .filter(x -> toRemove.stream().noneMatch(y -> x.getKey().equals(y.project)))
                .collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public boolean isSatisfiedBy(final ImmutableSet<RecipeVersionIdentifier> recipes) {
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

    public static DependencyGroup of(final ImmutableMap<RecipeIdentifier, SemanticVersionRequirement> dependencies) {
        return new DependencyGroup(dependencies);
    }

    public static DependencyGroup of() {
        return new DependencyGroup(ImmutableMap.of());
    }

    public static DependencyGroup of(final Dependency dependency, final Dependency... dependencies) {
        final ImmutableMap<RecipeIdentifier, SemanticVersionRequirement> x =
            Streams.concat(Stream.of(dependency), Arrays.stream(dependencies))
                .collect(ImmutableMap.toImmutableMap(i -> i.project, i -> i.requirement));
        return new DependencyGroup(x);
    }
}
