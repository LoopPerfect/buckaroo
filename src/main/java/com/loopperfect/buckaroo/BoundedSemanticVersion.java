package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Objects;

public final class BoundedSemanticVersion implements SemanticVersionRequirement {

    public final SemanticVersion bound;
    public final AboveOrBelow direction;

    private BoundedSemanticVersion(final SemanticVersion bound, final AboveOrBelow direction) {

        this.bound = Preconditions.checkNotNull(bound);
        this.direction = Preconditions.checkNotNull(direction);
    }

    @Override
    public boolean isSatisfiedBy(final SemanticVersion version) {
        switch (direction) {
            case ABOVE:
                return bound.compareTo(version) <= 0;
            case BELOW:
                return bound.compareTo(version) >= 0;
        }
        return false;
    }

    @Override
    public ImmutableSet<SemanticVersion> hints() {
        return ImmutableSet.of(bound);
    }

    @Override
    public String encode() {
        switch (direction) {
            case ABOVE:
                return ">=" + bound;
            case BELOW:
                return "<=" + bound;
        }
        return "";
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof BoundedSemanticVersion)) {
            return false;
        }

        final BoundedSemanticVersion other = (BoundedSemanticVersion) obj;

        return Objects.equals(bound, other.bound) &&
                Objects.equals(direction, other.direction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bound, direction);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("bound", bound)
                .add("direction", direction)
                .toString();
    }

    public static BoundedSemanticVersion of(final SemanticVersion bound, final AboveOrBelow direction) {
        return new BoundedSemanticVersion(bound, direction);
    }

    public static BoundedSemanticVersion atLeast(final SemanticVersion bound) {
        return new BoundedSemanticVersion(bound, AboveOrBelow.ABOVE);
    }

    public static BoundedSemanticVersion atMost(final SemanticVersion bound) {
        return new BoundedSemanticVersion(bound, AboveOrBelow.BELOW);
    }
}
