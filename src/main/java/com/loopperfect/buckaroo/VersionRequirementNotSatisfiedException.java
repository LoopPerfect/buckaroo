package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;

/**
 * Created by gaetano on 16/02/17.
 */
public final class VersionRequirementNotSatisfiedException extends DependencyResolverException {
    private final SemanticVersionRequirement requirement;

    VersionRequirementNotSatisfiedException(final Identifier id, final SemanticVersionRequirement requirement) {
        super(id, "Project " + id.name + " can't satisfy version requirement: "+ requirement.toString());
        this.requirement = Preconditions.checkNotNull(requirement);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof VersionRequirementNotSatisfiedException)) {
            return false;
        }

        final VersionRequirementNotSatisfiedException other = (VersionRequirementNotSatisfiedException) obj;

        return Objects.equals(id, other.id)
            && Objects.equals(requirement, other.requirement);
    }
}
