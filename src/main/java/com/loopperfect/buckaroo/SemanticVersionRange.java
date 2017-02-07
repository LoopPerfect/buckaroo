package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public final class SemanticVersionRange implements SemanticVersionRequirement {

    public final SemanticVersion minimumVersion;
    public final SemanticVersion maximumVersion;

    public SemanticVersionRange(final SemanticVersion minimumVersion, final SemanticVersion maximumVersion) {

        this.minimumVersion = Preconditions.checkNotNull(minimumVersion);
        this.maximumVersion = Preconditions.checkNotNull(maximumVersion);

        Preconditions.checkArgument(minimumVersion.compareTo(maximumVersion) <= 0);
    }

    public boolean isSatisfiedBy(SemanticVersion version) {
        return (minimumVersion.compareTo(version) <= 0) &&
                (maximumVersion.compareTo(version) >= 0);
    }

    public ImmutableSet<SemanticVersion> hints() {
        return ImmutableSet.of(minimumVersion, maximumVersion);
    }

    @Override
    public String encode() {
        return minimumVersion + "-" + maximumVersion;
    }
}
