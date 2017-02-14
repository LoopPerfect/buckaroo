package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.builder;

public final class Project {

    public final Identifier name;
    public final Optional<String> license;
    public final ImmutableMap<Identifier, SemanticVersionRequirement> dependencies;

    private Project(final Identifier name, final Optional<String> license, final ImmutableMap<Identifier, SemanticVersionRequirement> dependencies) {

        this.name = Preconditions.checkNotNull(name);
        this.license = Preconditions.checkNotNull(license);
        this.dependencies = Preconditions.checkNotNull(dependencies);
    }

    public Project addDependency(final Dependency dependency) {

        Preconditions.checkNotNull(dependency);

        if (dependencies.containsKey(dependency.project) &&
                dependencies.get(dependency.project).equals(dependency.versionRequirement)) {
            return this;
        }

        final ImmutableMap<Identifier, SemanticVersionRequirement> nextDependencies =
                ImmutableMap.copyOf(
                        FluentIterable.from(dependencies.entrySet())
                                .append(Maps.immutableEntry(dependency.project, dependency.versionRequirement)));

        return new Project(name, license, nextDependencies);
    }

    public Project removeDependency(final Identifier identifier) {
        Preconditions.checkNotNull(identifier);
        if (!dependencies.containsKey(identifier)) {
            return this;
        }
        final ImmutableMap<Identifier, SemanticVersionRequirement> nextDependencies =
                ImmutableMap.copyOf(dependencies.entrySet()
                        .stream()
                        .filter(x -> !x.getKey().equals(identifier))
                        .collect(Collectors.toList()));
        return new Project(name, license, nextDependencies);
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null || !(obj instanceof Project)) {
            return false;
        }

        Project other = (Project) obj;

        return Objects.equals(name, other.name) &&
                Objects.equals(license, other.license) &&
                Objects.equals(dependencies, other.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, license, dependencies);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("license", license)
                .add("dependencies", dependencies)
                .toString();
    }

    public static Project of(final Identifier name, final Optional<String> license, final ImmutableMap<Identifier, SemanticVersionRequirement> dependencies) {
        return new Project(name, license, dependencies);
    }
}
