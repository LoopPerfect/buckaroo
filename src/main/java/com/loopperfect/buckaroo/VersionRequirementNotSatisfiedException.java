package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;

/**
 * Created by gaetano on 16/02/17.
 */
public final class VersionRequirementNotSatisfiedException extends DependencyResolverException {

    public final SemanticVersionRequirement requirement;

    public VersionRequirementNotSatisfiedException(final Identifier id, final SemanticVersionRequirement requirement) {
        super(id, "Cannot satisfy " + id.name + "@"+ requirement.encode());
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

    @Override
    public int hashCode() {
        return Objects.hash(requirement, super.hashCode());
    }
}
