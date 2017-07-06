package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;

public final class RecipeVersionIdentifier {

    public final RecipeIdentifier project;
    public final SemanticVersion version;

    private RecipeVersionIdentifier(final RecipeIdentifier project, final SemanticVersion version) {
        this.project = Preconditions.checkNotNull(project);
        this.version = Preconditions.checkNotNull(version);
    }

    public String encode() {
        return project.encode() + "@" + version.encode();
    }

    public boolean equals(final RecipeVersionIdentifier other) {
        return this == other ||
            (Objects.equals(project, other.project) &&
                Objects.equals(version, other.version));
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null && obj instanceof RecipeVersionIdentifier &&
                equals((RecipeVersionIdentifier) obj);
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

    public static RecipeVersionIdentifier of(final RecipeIdentifier project, final SemanticVersion version) {
        return new RecipeVersionIdentifier(project, version);
    }
}
