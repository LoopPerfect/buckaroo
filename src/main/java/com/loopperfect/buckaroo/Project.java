package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Project {

    public final Optional<String> name;
    public final Optional<String> target;
    public final Optional<String> license;
    public final DependencyGroup dependencies;

    private Project(final Optional<String> name, final Optional<String> target, final Optional<String> license, final DependencyGroup dependencies) {

        this.name = Preconditions.checkNotNull(name);
        this.target = Preconditions.checkNotNull(target);
        this.license = Preconditions.checkNotNull(license);
        this.dependencies = Preconditions.checkNotNull(dependencies);
    }

    public Project addDependency(final Dependency dependency) {
        Preconditions.checkNotNull(dependency);
        return new Project(name, target, license, dependencies.add(dependency));
    }

    public Project addDependencies(final List<Dependency> dependencies) {
        Preconditions.checkNotNull(dependencies);
        return new Project(name, target, license, this.dependencies.add(dependencies));
    }

    public Project removeDependencies(final List<Dependency> dependencies) {

        Preconditions.checkNotNull(dependencies);

        return new Project(name, target, license, this.dependencies.remove(dependencies));
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null || !(obj instanceof Project)) {
            return false;
        }

        final Project other = (Project) obj;

        return Objects.equals(name, other.name) &&
            Objects.equals(target, other.target) &&
            Objects.equals(license, other.license) &&
            Objects.equals(dependencies, other.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, target, license, dependencies);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("target", target)
            .add("license", license)
            .add("dependencies", dependencies)
            .toString();
    }

    public static Project of(final Optional<String> name, final Optional<String> target, final Optional<String> license, final DependencyGroup dependencies) {
        return new Project(name, target, license, dependencies);
    }

    public static Project of(final Optional<String> name) {
        return new Project(name, Optional.empty(), Optional.empty(), DependencyGroup.of());
    }

    public static Project of(final String name) {
        return new Project(Optional.of(name), Optional.empty(), Optional.empty(), DependencyGroup.of());
    }

    public static Project of(final String name, final DependencyGroup dependencies) {
        return new Project(Optional.of(name), Optional.empty(), Optional.empty(), dependencies);
    }

    public static Project of() {
        return new Project(Optional.empty(), Optional.empty(), Optional.empty(), DependencyGroup.of());
    }
}
