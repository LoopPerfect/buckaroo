package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Optional;

public final class AnySemanticVersion implements SemanticVersionRequirement {

    public final Optional<Integer> major;
    public final Optional<Integer> minor;

    private AnySemanticVersion(final Optional<Integer> major, final Optional<Integer> minor) {
        super();
        this.major = Preconditions.checkNotNull(major);
        this.minor = Preconditions.checkNotNull(minor);
    }

    @Override
    public boolean isSatisfiedBy(final SemanticVersion version) {
        if (major.isPresent()) {
            if (major.get().intValue() != version.major) {
                return false;
            }
        }
        if (minor.isPresent()) {
            if (minor.get().intValue() != version.minor) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableSet<SemanticVersion> hints() {
        return ImmutableSet.of();
    }

    @Override
    public String encode() {
        final StringBuilder b = new StringBuilder();
        if (major.isPresent()) {
            b.append(major.get());
            b.append(".");
        }
        if (minor.isPresent()) {
            b.append(minor.get());
            b.append(".");
        }
        b.append("*");
        return b.toString();
    }

    public boolean equals(final AnySemanticVersion other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(major, other.major) && Objects.equals(minor, other.minor);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof AnySemanticVersion && equals((AnySemanticVersion)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }

    public static AnySemanticVersion of() {
        return new AnySemanticVersion(Optional.empty(), Optional.empty());
    }

    public static AnySemanticVersion of(final int major) {
        return new AnySemanticVersion(Optional.of(major), Optional.empty());
    }

    public static AnySemanticVersion of(final int major, final int minor) {
        return new AnySemanticVersion(Optional.of(major), Optional.of(minor));
    }
}
