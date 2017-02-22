package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;

public final class RecipeIdentifier {

    public final Identifier project;
    public final SemanticVersion version;

    private RecipeIdentifier(final Identifier project, final SemanticVersion version) {
        this.project = Preconditions.checkNotNull(project);
        this.version = Preconditions.checkNotNull(version);
    }

    public String encode() {
        return project.name + "@" + version.toString();
    }

    public boolean equals(final RecipeIdentifier other) {
        return this == other ||
                (Objects.equals(project, other.project) &&
                        Objects.equals(version, other.version));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof RecipeIdentifier)) {
            return false;
        }
        return equals((RecipeIdentifier) obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, version);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("project", project)
                .add("version", version)
                .toString();
    }

    public static RecipeIdentifier of(final Identifier project, final SemanticVersion version) {
        return new RecipeIdentifier(project, version);
    }
}
