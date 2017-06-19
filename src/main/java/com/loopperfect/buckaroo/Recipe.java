package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Objects;

public final class Recipe {

    public final String name;
    public final String url;
    public final ImmutableMap<SemanticVersion, RecipeVersion> versions;

    private Recipe(final String name, final String url, final ImmutableMap<SemanticVersion, RecipeVersion> versions) {
        super();
        this.name = Preconditions.checkNotNull(name);
        this.url = Preconditions.checkNotNull(url);
        this.versions = Preconditions.checkNotNull(versions);
    }

    @Override
    public boolean equals(final Object obj) {

        if (obj == null || !(obj instanceof Recipe)) {
            return false;
        }

        final Recipe other = (Recipe) obj;

        return Objects.equals(name, other.name) &&
            Objects.equals(url, other.url) &&
            Objects.equals(versions, other.versions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, versions);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("url", url)
            .add("versions", versions)
            .toString();
    }

    public static Recipe of(
            final String name,
            final String url,
            final ImmutableMap<SemanticVersion, RecipeVersion> versions) {
        return new Recipe(name, url, versions);
    }
}
