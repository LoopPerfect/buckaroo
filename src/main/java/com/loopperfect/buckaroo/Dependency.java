package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.util.Map;

public final class Dependency {

    public final Identifier project;
    public final SemanticVersionRequirement versionRequirement;

    private Dependency(final Identifier project, final SemanticVersionRequirement versionRequirement) {

        this.project = Preconditions.checkNotNull(project);
        this.versionRequirement = Preconditions.checkNotNull(versionRequirement);
    }

    public boolean isSatisfiedBy(final Recipe recipe) {

        Preconditions.checkNotNull(recipe);

        return Objects.equal(recipe.name, project) &&
                recipe.versions.keySet().stream().anyMatch(x -> versionRequirement.isSatisfiedBy(x));
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof Dependency)) {
            return false;
        }

        Dependency other = (Dependency) obj;

        return Objects.equal(project, other.project) &&
                Objects.equal(versionRequirement, other.versionRequirement);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(project, versionRequirement);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("project", project)
                .add("versionRequirement", versionRequirement)
                .toString();
    }

    public static Dependency of(final Identifier project, final SemanticVersionRequirement versionRequirement) {
        return new Dependency(project, versionRequirement);
    }
}
