package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class RecipeVersion {

    public final Either<GitCommit, RemoteArchive> source;
    public final Optional<String> target;
    public final DependencyGroup dependencies;
    public final Optional<Resource> buckResource;

    private RecipeVersion(
            final Either<GitCommit, RemoteArchive> source,
            final Optional<String> target,
            final DependencyGroup dependencies,
            final Optional<Resource> buckResource) {

        this.source = Preconditions.checkNotNull(source);
        this.target = Preconditions.checkNotNull(target);
        this.dependencies = Preconditions.checkNotNull(dependencies);
        this.buckResource = Preconditions.checkNotNull(buckResource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, dependencies);
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

        return Objects.equals(source, other.source) &&
                Objects.equals(target, other.target) &&
                Objects.equals(dependencies, other.dependencies) &&
                Objects.equals(buckResource, other.buckResource);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("source", source)
                .add("target", target)
                .add("dependencies", dependencies)
                .add("buckResource", buckResource)
                .toString();
    }

    public static RecipeVersion of(final Either<GitCommit, RemoteArchive> source, final Optional<String> target,
                                   final DependencyGroup dependencies, final Optional<Resource> buckResource) {
        return new RecipeVersion(source, target, dependencies, buckResource);
    }

    public static RecipeVersion of(final RemoteArchive source, final Optional<String> target,
                                   final DependencyGroup dependencies, final Optional<Resource> buckResource) {
        return new RecipeVersion(Either.right(source), target, dependencies, buckResource);
    }

    public static RecipeVersion of(final GitCommit source, final Optional<String> target,
            final DependencyGroup dependencies, final Optional<Resource> buckResource) {
        return new RecipeVersion(Either.left(source), target, dependencies, buckResource);
    }

    public static RecipeVersion of(final GitCommit source, final Optional<String> target,
                                   final DependencyGroup dependencies) {
        return new RecipeVersion(Either.left(source), target, dependencies, Optional.empty());
    }

    public static RecipeVersion of(final GitCommit source, final DependencyGroup dependencies) {
        return new RecipeVersion(Either.left(source), Optional.empty(), dependencies, Optional.empty());
    }

    public static RecipeVersion of(final GitCommit source, final String target) {
        return new RecipeVersion(Either.left(source), Optional.of(target), DependencyGroup.of(), Optional.empty());
    }

    public static RecipeVersion of(final GitCommit source) {
        return new RecipeVersion(Either.left(source), Optional.empty(), DependencyGroup.of(), Optional.empty());
    }
}
