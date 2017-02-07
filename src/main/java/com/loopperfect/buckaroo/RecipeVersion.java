package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class RecipeVersion {

    public final String url;
    public final Optional<String> buckUrl;
    public final String target;

    private RecipeVersion(final String url, final Optional<String> buckUrl, final String target) {

        this.url = Preconditions.checkNotNull(url);
        this.buckUrl = Preconditions.checkNotNull(buckUrl);
        this.target = Preconditions.checkNotNull(target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, buckUrl, target);
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null || !(obj instanceof RecipeVersion)) {
            return false;
        }

        final RecipeVersion other = (RecipeVersion) obj;

        return Objects.equals(url, other.url) &&
                Objects.equals(buckUrl, other.buckUrl) &&
                Objects.equals(target, other.target);
    }

    public static RecipeVersion of(final String url, final Optional<String> buckUrl, final String target) {
        return new RecipeVersion(url, buckUrl, target);
    }

    public static RecipeVersion of(final String url, final String target) {
        return new RecipeVersion(url, Optional.empty(), target);
    }

    public static RecipeVersion of(final String url, final String buckUrl, final String target) {
        return new RecipeVersion(url, Optional.of(buckUrl), target);
    }
}
