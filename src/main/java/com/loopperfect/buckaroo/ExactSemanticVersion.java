package com.loopperfect.buckaroo;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public final class ExactSemanticVersion implements SemanticVersionRequirement {

    public final SemanticVersion semanticVersion;

    private ExactSemanticVersion(final SemanticVersion semanticVersion) {
        this.semanticVersion = Preconditions.checkNotNull(semanticVersion);
    }

    public boolean isSatisfiedBy(final SemanticVersion version) {
        return Objects.equal(this.semanticVersion, version);
    }

    public ImmutableSet<SemanticVersion> hints() {
        return ImmutableSet.of(semanticVersion);
    }

    public static ExactSemanticVersion of(final SemanticVersion semanticVersion) {
        return new ExactSemanticVersion(semanticVersion);
    }
}
