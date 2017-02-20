package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Optional;

public final class RecipeVersion {

    public final GitCommit gitCommit;
    public final Optional<String> buckUrl;
    public final String target;
    public final DependencyGroup dependencies;

    private RecipeVersion(final GitCommit gitCommit, final Optional<String> buckUrl, final String target, final DependencyGroup dependencies) {

        this.gitCommit = Preconditions.checkNotNull(gitCommit);
        this.buckUrl = Preconditions.checkNotNull(buckUrl);
        this.target = Preconditions.checkNotNull(target);
        this.dependencies = Preconditions.checkNotNull(dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gitCommit, buckUrl, target, dependencies);
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null || !(obj instanceof RecipeVersion)) {
            return false;
        }

        final RecipeVersion other = (RecipeVersion) obj;

        return Objects.equals(gitCommit, other.gitCommit) &&
                Objects.equals(buckUrl, other.buckUrl) &&
                Objects.equals(target, other.target) &&
                Objects.equals(dependencies, other.dependencies);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("gitCommit", gitCommit)
                .add("buckUrl", buckUrl)
                .add("target", target)
                .add("dependencies", dependencies)
                .toString();
    }

    public static RecipeVersion of(final GitCommit gitCommit, final Optional<String> buckUrl, final String target, final DependencyGroup dependencies) {
        return new RecipeVersion(gitCommit, buckUrl, target, dependencies);
    }

    public static RecipeVersion of(final GitCommit gitCommit, final String target) {
        return new RecipeVersion(gitCommit, Optional.empty(), target, DependencyGroup.of());
    }

    public static RecipeVersion of(final GitCommit gitCommit, final String buckUrl, final String target) {
        return new RecipeVersion(gitCommit, Optional.of(buckUrl), target, DependencyGroup.of());
    }
}
