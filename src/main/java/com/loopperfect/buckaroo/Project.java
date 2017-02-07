package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Optional;

public final class Project {

    public final Identifier name;
    public final Optional<String> license;
    public final ImmutableSet<Dependency> dependencies;

    public Project(final Identifier name, final Optional<String> license, final ImmutableSet<Dependency> dependencies) {

        this.name = Preconditions.checkNotNull(name);
        this.license = Preconditions.checkNotNull(license);
        this.dependencies = Preconditions.checkNotNull(dependencies);
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
}
