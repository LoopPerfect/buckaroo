package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.*;

public final class SemanticVersions {

    private SemanticVersions() {

    }

    /**
     * Figures out the best version to use given a list of available versions and requirements.
     *
     * @param availableVersions The versions available. Usually read from a cook-book
     * @param requirements The version requirements
     * @return The best version to use, if such a version exists
     */
    public static Optional<SemanticVersion> resolve(
            final ImmutableSet<SemanticVersion> availableVersions,
            final ImmutableSet<SemanticVersionRequirement> requirements) {

        Preconditions.checkNotNull(availableVersions);
        Preconditions.checkNotNull(requirements);

        return availableVersions.stream()
                .filter(x -> requirements.stream().allMatch(y -> y.isSatisfiedBy(x)))
                .sorted(Comparator.reverseOrder())
                .findFirst();
    }
}
