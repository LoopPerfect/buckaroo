package com.loopperfect.buckaroo.versioning;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.SemanticVersion;
import com.loopperfect.buckaroo.SemanticVersionRequirement;

import java.util.Objects;
import java.util.Optional;

public final class WildcardVersion implements SemanticVersionRequirement {

    private final Optional<Integer> major;
    private final Optional<Integer> minor;
    private final Optional<Integer> patch;
    private final Optional<Integer> increment;

    private WildcardVersion(final Optional<Integer> major, final Optional<Integer> minor, final Optional<Integer> patch, final Optional<Integer> increment) {
        super();

        this.major = Preconditions.checkNotNull(major);
        this.minor = Preconditions.checkNotNull(minor);
        this.patch = Preconditions.checkNotNull(patch);
        this.increment = Preconditions.checkNotNull(increment);

        Preconditions.checkArgument(major.map(x -> x >= 0).orElse(true));
        Preconditions.checkArgument(minor.map(x -> x >= 0).orElse(true));
        Preconditions.checkArgument(patch.map(x -> x >= 0).orElse(true));
        Preconditions.checkArgument(increment.map(x -> x >= 0).orElse(true));
    }

    @Override
    public boolean isSatisfiedBy(final SemanticVersion version) {
        return major.map(x -> version.major == x).orElse(true) &&
            minor.map(x -> version.minor == x).orElse(true) &&
            patch.map(x -> version.patch == x).orElse(true) &&
            increment.map(x -> version.increment == x).orElse(true);
    }

    @Override
    public String encode() {
        if (increment.isPresent()) {
            return String.join(".",
                major.map(Object::toString).orElse("*"),
                minor.map(Object::toString).orElse("*"),
                patch.map(Object::toString).orElse("*"),
                increment.toString());
        }

        if (patch.isPresent()) {
            return String.join(".",
                major.map(Object::toString).orElse("*"),
                minor.map(Object::toString).orElse("*"),
                patch.toString());
        }

        if (minor.isPresent()) {
            return String.join(".",
                major.map(Object::toString).orElse("*"),
                minor.toString());
        }

        if (major.isPresent()) {
            return major.toString();
        }

        return "*";
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof WildcardVersion) {
            final WildcardVersion other = (WildcardVersion) obj;

            return Objects.equals(major, other.major) &&
                Objects.equals(minor, other.minor) &&
                Objects.equals(patch, other.patch) &&
                Objects.equals(increment, other.increment);

        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, increment);
    }

    @Override
    public String toString() {
        return encode();
    }

    public static WildcardVersion of() {
        return new WildcardVersion(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static WildcardVersion of(final int major) {
        return new WildcardVersion(Optional.of(major), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static WildcardVersion of(final int major, int minor) {
        return new WildcardVersion(Optional.of(major), Optional.of(minor), Optional.empty(), Optional.empty());
    }

    public static WildcardVersion of(final int major, int minor, int patch) {
        return new WildcardVersion(Optional.of(major), Optional.of(minor), Optional.of(patch), Optional.empty());
    }
}
