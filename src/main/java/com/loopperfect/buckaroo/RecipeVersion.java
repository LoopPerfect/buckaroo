package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class RecipeVersion {

    public final GitCommit gitCommit;
    public final Optional<String> buckUrl;
    public final String target;

    private RecipeVersion(final GitCommit gitCommit, final Optional<String> buckUrl, final String target) {

        this.gitCommit = Preconditions.checkNotNull(gitCommit);
        this.buckUrl = Preconditions.checkNotNull(buckUrl);
        this.target = Preconditions.checkNotNull(target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gitCommit, buckUrl, target);
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null || !(obj instanceof RecipeVersion)) {
            return false;
        }

        final RecipeVersion other = (RecipeVersion) obj;

        return Objects.equals(gitCommit, other.gitCommit) &&
                Objects.equals(buckUrl, other.buckUrl) &&
                Objects.equals(target, other.target);
    }

    public static RecipeVersion of(final GitCommit gitCommit, final Optional<String> buckUrl, final String target) {
        return new RecipeVersion(gitCommit, buckUrl, target);
    }

    public static RecipeVersion of(final GitCommit gitCommit, final String target) {
        return new RecipeVersion(gitCommit, Optional.empty(), target);
    }

    public static RecipeVersion of(final GitCommit gitCommit, final String buckUrl, final String target) {
        return new RecipeVersion(gitCommit, Optional.of(buckUrl), target);
    }
}
