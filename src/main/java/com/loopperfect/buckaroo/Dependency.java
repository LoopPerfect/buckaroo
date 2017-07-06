package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public final class Dependency {

    public final RecipeIdentifier project;
    public final SemanticVersionRequirement requirement;

    private Dependency(final RecipeIdentifier project, final SemanticVersionRequirement requirement) {

        this.project = Preconditions.checkNotNull(project);
        this.requirement = Preconditions.checkNotNull(requirement);
    }

    public String encode() {
        return project.encode() + "@" + requirement.encode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof Dependency)) {
            return false;
        }

        Dependency other = (Dependency) obj;

        return Objects.equal(project, other.project) &&
            Objects.equal(requirement, other.requirement);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(project, requirement);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("project", project)
            .add("requirement", requirement)
            .toString();
    }

    public static Dependency of(final RecipeIdentifier project, final SemanticVersionRequirement versionRequirement) {
        return new Dependency(project, versionRequirement);
    }
}
