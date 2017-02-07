package com.loopperfect.buckaroo;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

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

    public static ExactSemanticVersion of(final SemanticVersion semanticVersion) {
        return new ExactSemanticVersion(ImmutableSet.of(semanticVersion));
    }

    public static ExactSemanticVersion of(final ImmutableSet<SemanticVersion> semanticVersions) {
        return new ExactSemanticVersion(semanticVersions);
    }
}
