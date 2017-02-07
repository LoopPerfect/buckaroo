package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableSet;

public final class AnySemanticVersion implements SemanticVersionRequirement {

    private AnySemanticVersion() {

    }

    @Override
    public boolean isSatisfiedBy(final SemanticVersion version) {
        return true;
    }

    @Override
    public ImmutableSet<SemanticVersion> hints() {
        return ImmutableSet.of();
    }

    @Override
    public String encode() {
        return "*";
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof AnySemanticVersion;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public static AnySemanticVersion of() {
        return new AnySemanticVersion();
    }
}
