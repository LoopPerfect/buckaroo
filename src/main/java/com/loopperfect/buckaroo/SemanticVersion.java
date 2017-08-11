package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.versioning.VersioningParsers;

import java.util.Objects;
import java.util.Optional;

public final class SemanticVersion implements Comparable<SemanticVersion> {

    public final int major;
    public final int minor;
    public final int patch;
    public final int increment;

    private SemanticVersion(final int major, final int minor, final int patch, final int increment) {

        Preconditions.checkArgument(major >= 0, "major version must be non-negative");
        Preconditions.checkArgument(minor >= 0, "minor version must be non-negative");
        Preconditions.checkArgument(patch >= 0, "patch version must be non-negative");
        Preconditions.checkArgument(increment >= 0, "increment version must be non-negative");

        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.increment = increment;
    }

    public int compareTo(final SemanticVersion other) {

        Preconditions.checkNotNull(other);

        int majorComparison = Integer.compare(major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }

        int minorComparison = Integer.compare(minor, other.minor);
        if (minorComparison != 0) {
            return minorComparison;
        }

        int patchComparison = Integer.compare(patch, other.patch);
        if (patchComparison != 0) {
            return patchComparison;
        }

        return Integer.compare(increment, other.increment);
    }

    public String encode() {
        if (increment == 0) {
            return major + "." + minor + "." + patch;
        }
        return major + "." + minor + "." + patch + "." + increment;
    }

    public boolean equals(final SemanticVersion other) {
        Preconditions.checkNotNull(other);
        return this == other ||
            ((major == other.major) &&
                (minor == other.minor) &&
                (patch == other.patch) &&
                (increment == other.increment));
    }

    @Override
    public boolean equals(final Object o) {
        return o == this || !(o == null || !(o instanceof SemanticVersion)) && equals((SemanticVersion) o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, increment);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + "." + increment;
    }

    public static SemanticVersion of(final int major, final int minor, final int patch, final int increment) {
        return new SemanticVersion(major, minor, patch, increment);
    }

    public static SemanticVersion of(final int major, final int minor, final int patch) {
        return new SemanticVersion(major, minor, patch, 0);
    }

    public static SemanticVersion of(final int major, final int minor) {
        return new SemanticVersion(major, minor, 0, 0);
    }

    public static SemanticVersion of(final int major) {
        return new SemanticVersion(major, 0, 0, 0);
    }

    public static Optional<SemanticVersion> parse(final String x) {

        Preconditions.checkNotNull(x);

        try {
            return Optional.of(VersioningParsers.semanticVersionParser.parse(x));
        } catch (final Throwable e) {
            return Optional.empty();
        }
    }
}