package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SemanticVersion implements Comparable<SemanticVersion> {

    public final int major;
    public final int minor;
    public final int patch;

    private SemanticVersion(final int major, final int minor, final int patch) {

        Preconditions.checkArgument(major >= 0, "major version must be non-negative");
        Preconditions.checkArgument(minor >= 0, "minor version must be non-negative");
        Preconditions.checkArgument(patch >= 0, "patch version must be non-negative");

        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int compareTo(final SemanticVersion other) {

        int majorComparison = Integer.compare(major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }

        int minorComparison = Integer.compare(minor, other.minor);
        if (minorComparison != 0) {
            return minorComparison;
        }

        return Integer.compare(patch, other.patch);
    }

    public boolean equals(final SemanticVersion other) {
        Preconditions.checkNotNull(other);
        return this == other ||
            ((major == other.major) &&
                (minor == other.minor) &&
                (patch == other.patch));
    }

    @Override
    public boolean equals(final Object o) {

        if (o == this) {
            return true;
        }

        if (o == null || !(o instanceof SemanticVersion)) {
            return false;
        }

        return equals((SemanticVersion) o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    public static SemanticVersion of(final int major, final int minor, final int patch) {
        return new SemanticVersion(major, minor, patch);
    }

    public static SemanticVersion of(final int major, final int minor) {
        return new SemanticVersion(major, minor, 0);
    }

    public static SemanticVersion of(final int major) {
        return new SemanticVersion(major, 0, 0);
    }

    public static Optional<SemanticVersion> parse(final String x) {

        Preconditions.checkNotNull(x);

        final String[] parts = x.trim().split(Pattern.quote("."), -1); // -1 enables empty parts

        if (parts.length == 0 || parts.length > 3) {
            return Optional.empty();
        }

        final List<Integer> numbers = new ArrayList<>();

        for (final String part : parts) {
            try {
                numbers.add(Integer.parseUnsignedInt(part));
            } catch (final NumberFormatException e) {
                return Optional.empty();
            }
        }

        if (numbers.size() == 3) {
            return Optional.of(SemanticVersion.of(numbers.get(0), numbers.get(1), numbers.get(2)));
        }

        if (numbers.size() == 2) {
            return Optional.of(SemanticVersion.of(numbers.get(0), numbers.get(1)));
        }

        if (numbers.size() == 1) {
            return Optional.of(SemanticVersion.of(numbers.get(0)));
        }

        return Optional.empty();
    }
}