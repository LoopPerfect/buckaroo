package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Objects;

public final class ResolvedPlatformDependencies {

    public final String platform;
    public final ImmutableList<RecipeIdentifier> dependencies;

    private ResolvedPlatformDependencies(final String platform, final ImmutableList<RecipeIdentifier> dependencies) {

        Preconditions.checkNotNull(platform);
        Preconditions.checkArgument(!platform.contains("\n"));
        Preconditions.checkNotNull(dependencies);
        Preconditions.checkArgument(dependencies.stream().noneMatch(Objects::isNull));

        this.platform = platform;
        this.dependencies = dependencies;
    }

    public boolean equals(final ResolvedPlatformDependencies other) {
        Preconditions.checkNotNull(other);
        return this == other ||
            Objects.equals(platform, other.platform) &&
                Objects.equals(dependencies, other.dependencies);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null &&
                obj instanceof ResolvedPlatformDependencies &&
                equals((ResolvedPlatformDependencies) obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, dependencies);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("platform", platform)
            .add("dependencies", dependencies)
            .toString();
    }

    public static final ResolvedPlatformDependencies of(final String platform, final ImmutableList<RecipeIdentifier> dependencies) {
        return new ResolvedPlatformDependencies(platform, dependencies);
    }
}
