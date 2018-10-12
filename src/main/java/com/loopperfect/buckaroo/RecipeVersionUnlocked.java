package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public final class RecipeVersionUnlocked {

    public final Either<GitCommit, RemoteArchiveUnlocked> source;
    public final Optional<String> target;
    public final Optional<DependencyGroup> dependencies;
    public final Optional<URI> buckResource;

    private RecipeVersionUnlocked(
        final Either<GitCommit, RemoteArchiveUnlocked> source,
        final Optional<String> target,
        final Optional<DependencyGroup> dependencies,
        final Optional<URI> buckResource) {

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

    public static RecipeVersionUnlocked of(final Either<GitCommit, RemoteArchiveUnlocked> source, final Optional<String> target, final Optional<URI> buckResource) {
        return new RecipeVersionUnlocked(source, target, Optional.empty(), buckResource);
    }

    public static RecipeVersionUnlocked of(final Either<GitCommit, RemoteArchiveUnlocked> source, final Optional<String> target,
                                   final Optional<DependencyGroup> dependencies, final Optional<URI> buckResource) {
        return new RecipeVersionUnlocked(source, target, dependencies, buckResource);
    }

    public static RecipeVersionUnlocked of(final Either<GitCommit, RemoteArchiveUnlocked> source, final Optional<String> target,
                                   final DependencyGroup dependencies, final Optional<URI> buckResource) {
        return new RecipeVersionUnlocked(source, target, Optional.of(dependencies), buckResource);
    }

    public static RecipeVersionUnlocked of(final RemoteArchiveUnlocked source, final Optional<String> target,
                                   final DependencyGroup dependencies, final Optional<URI> buckResource) {
        return new RecipeVersionUnlocked(Either.right(source), target, Optional.of(dependencies), buckResource);
    }

    public static RecipeVersionUnlocked of(final GitCommit source, final Optional<String> target,
                                   final DependencyGroup dependencies, final Optional<URI> buckResource) {
        return new RecipeVersionUnlocked(Either.left(source), target, Optional.of(dependencies), buckResource);
    }

    @Deprecated
    public static RecipeVersionUnlocked of(final GitCommit source, final Optional<String> target,
                                   final DependencyGroup dependencies) {
        return new RecipeVersionUnlocked(Either.left(source), target, Optional.of(dependencies), Optional.empty());
    }

    @Deprecated
    public static RecipeVersionUnlocked of(final GitCommit source, final DependencyGroup dependencies) {
        return new RecipeVersionUnlocked(Either.left(source), Optional.empty(), Optional.of(dependencies), Optional.empty());
    }

    public static RecipeVersionUnlocked of(final RemoteArchiveUnlocked source) {
        return new RecipeVersionUnlocked(Either.right(source), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static RecipeVersionUnlocked of(final GitCommit source, final String target) {
        return new RecipeVersionUnlocked(Either.left(source), Optional.of(target), Optional.empty(), Optional.empty());
    }

    public static RecipeVersionUnlocked of(final GitCommit source) {
        return new RecipeVersionUnlocked(Either.left(source), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
