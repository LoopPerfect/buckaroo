package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableSet;

public final class AnySemanticVersion implements SemanticVersionRequirement {

    private AnySemanticVersion() {

    }

    public boolean isSatisfiedBy(SemanticVersion version) {
        return true;
    }

    public ImmutableSet<SemanticVersion> hints() {
        return ImmutableSet.of();
    }

    public static AnySemanticVersion of() {
        return new AnySemanticVersion();
    }
}
