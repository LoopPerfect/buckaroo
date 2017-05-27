package com.loopperfect.buckaroo.versioning;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.loopperfect.buckaroo.SemanticVersion;
import com.loopperfect.buckaroo.SemanticVersionRequirement;

import java.util.Objects;
import java.util.stream.Collectors;

public final class ExactSemanticVersion implements SemanticVersionRequirement {

    public final ImmutableSet<SemanticVersion> semanticVersions;

    private ExactSemanticVersion(final ImmutableSet<SemanticVersion> semanticVersions) {
        this.semanticVersions = Preconditions.checkNotNull(semanticVersions);
    }

    public boolean isSatisfiedBy(final SemanticVersion version) {
        return this.semanticVersions.contains(version);
    }

    public ImmutableSet<SemanticVersion> hints() {
        return semanticVersions;
    }

    @Override
    public String encode() {
        if (semanticVersions.size() == 1) {
            return semanticVersions.iterator().next().toString();
        }
        return "[" +
            semanticVersions.stream()
                .map(x -> x.toString())
                .collect(Collectors.joining(", ")) +
            "]";
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof ExactSemanticVersion)) {
            return false;
        }

        final ExactSemanticVersion other = (ExactSemanticVersion) obj;

        return Objects.equals(semanticVersions, other.semanticVersions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semanticVersions);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("semanticVersions", semanticVersions)
            .toString();
    }

    public static ExactSemanticVersion of(final SemanticVersion semanticVersion) {
        return new ExactSemanticVersion(ImmutableSet.of(semanticVersion));
    }

    public static ExactSemanticVersion of(final ImmutableSet<SemanticVersion> semanticVersions) {
        return new ExactSemanticVersion(semanticVersions);
    }

    public static ExactSemanticVersion of(final SemanticVersion... semanticVersions) {
        return new ExactSemanticVersion(ImmutableSet.copyOf(semanticVersions));
    }
}
