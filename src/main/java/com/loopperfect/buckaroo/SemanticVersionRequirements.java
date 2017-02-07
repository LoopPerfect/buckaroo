package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Optional;

public final class SemanticVersionRequirements {

    private SemanticVersionRequirements() {

    }

    public static Optional<SemanticVersionRequirement> parse(final String x) {

        Preconditions.checkNotNull(x);

        // We currently only handle the simple case of one explicit version
        // TODO: Support the versions DSL

        return SemanticVersion.parse(x).map(ExactSemanticVersion::of);
    }
}
