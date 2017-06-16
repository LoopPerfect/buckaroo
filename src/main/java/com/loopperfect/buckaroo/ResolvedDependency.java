package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a resolved dependency that can be included in a lock file.
 * This should contain all of the information required to download the
 * source-code and generate any BUCK files.
 */
public final class ResolvedDependency {

    public final Either<GitCommit, RemoteArchive> source;
    public final Optional<String> target;
    public final Optional<RemoteFile> buckResource;
    public final ImmutableList<RecipeIdentifier> dependencies;

    private ResolvedDependency(
        final Either<GitCommit, RemoteArchive> source,
        final Optional<String> target,
        final Optional<RemoteFile> buckResource,
        final ImmutableList<RecipeIdentifier> dependencies) {

        super();

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(buckResource);
        Preconditions.checkNotNull(dependencies);

        this.source = source;
        this.target = target;
        this.buckResource = buckResource;
        this.dependencies = dependencies;
    }

    public boolean equals(final ResolvedDependency other) {
        return Objects.equals(source, other.source) &&
            Objects.equals(target, other.target) &&
            Objects.equals(buckResource, other.buckResource) &&
            Objects.equals(dependencies, other.dependencies);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof ResolvedDependency &&
            equals((ResolvedDependency) obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, buckResource, dependencies);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("source", source)
            .add("target", target)
            .add("buckResource", buckResource)
            .add("dependencies", dependencies)
            .toString();
    }

    public static ResolvedDependency of(
        final Either<GitCommit, RemoteArchive> source,
        final Optional<String> target,
        final Optional<RemoteFile> buckResource,
        final ImmutableList<RecipeIdentifier> dependencies) {
        return new ResolvedDependency(source, target, buckResource, dependencies);
    }

    public static ResolvedDependency of(final Either<GitCommit, RemoteArchive> source) {
        return new ResolvedDependency(source, Optional.empty(), Optional.empty(), ImmutableList.of());
    }

    public static ResolvedDependency from(final RecipeVersion recipeVersion) {
        Preconditions.checkNotNull(recipeVersion);
        return of(
            recipeVersion.source,
            recipeVersion.target,
            recipeVersion.buckResource,
            recipeVersion.dependencies.orElse(DependencyGroup.of()).entries()
                .stream()
                .map(x -> x.project)
                .collect(ImmutableList.toImmutableList()));
    }
}
