package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class RecipeVersion {

    public final GitCommit gitCommit;
    public final Optional<String> target;
    public final DependencyGroup dependencies;
    public final Optional<Resource> buckResource;

    private RecipeVersion(
            final GitCommit gitCommit,
            final Optional<String> target,
            final DependencyGroup dependencies,
            final Optional<Resource> buckResource) {

        this.gitCommit = Preconditions.checkNotNull(gitCommit);
        this.target = Preconditions.checkNotNull(target);
        this.dependencies = Preconditions.checkNotNull(dependencies);
        this.buckResource = Preconditions.checkNotNull(buckResource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gitCommit, target, dependencies);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof RecipeVersion)) {
            return false;
        }

        final RecipeVersion other = (RecipeVersion) obj;

        return Objects.equals(gitCommit, other.gitCommit) &&
                Objects.equals(target, other.target) &&
                Objects.equals(dependencies, other.dependencies) &&
                Objects.equals(buckResource, other.buckResource);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("gitCommit", gitCommit)
                .add("target", target)
                .add("dependencies", dependencies)
                .add("buckResource", buckResource)
                .toString();
    }

    public static RecipeVersion of(final GitCommit gitCommit, final Optional<String> target,
            final DependencyGroup dependencies, final Optional<Resource> buckResource) {
        return new RecipeVersion(gitCommit, target, dependencies, buckResource);
    }

    public static RecipeVersion of(final GitCommit gitCommit, final Optional<String> target,
                                   final DependencyGroup dependencies) {
        return new RecipeVersion(gitCommit, target, dependencies, Optional.empty());
    }

    public static RecipeVersion of(final GitCommit gitCommit, final DependencyGroup dependencies) {
        return new RecipeVersion(gitCommit, Optional.empty(), dependencies, Optional.empty());
    }

    public static RecipeVersion of(final GitCommit gitCommit, final String target) {
        return new RecipeVersion(gitCommit, Optional.of(target), DependencyGroup.of(), Optional.empty());
    }

    public static RecipeVersion of(final GitCommit gitCommit) {
        return new RecipeVersion(gitCommit, Optional.empty(), DependencyGroup.of(), Optional.empty());
    }
}
