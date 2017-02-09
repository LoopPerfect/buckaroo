package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public final class SemanticVersionsTest {

    @org.junit.Test
    public void resolve1() throws Exception {

        ImmutableSet<SemanticVersion> availableVersions =
                ImmutableSet.of(
                        SemanticVersion.of(1),
                        SemanticVersion.of(3, 2),
                        SemanticVersion.of(4, 1),
                        SemanticVersion.of(27, 3, 0));

        ImmutableSet<SemanticVersionRequirement> requirements =
                ImmutableSet.of(
                        AnySemanticVersion.of(),
                        SemanticVersionRange.of(SemanticVersion.of(3), SemanticVersion.of(7)));

        Optional<SemanticVersion> suggested = SemanticVersions.resolve(availableVersions, requirements);

        assertEquals(suggested, Optional.of(SemanticVersion.of(4, 1)));
    }

    @org.junit.Test
    public void resolve2() throws Exception {

        ImmutableSet<SemanticVersion> availableVersions =
                ImmutableSet.of(
                        SemanticVersion.of(1),
                        SemanticVersion.of(3, 2),
                        SemanticVersion.of(4, 1),
                        SemanticVersion.of(27, 3, 0));

        ImmutableSet<SemanticVersionRequirement> requirements =
                ImmutableSet.of(
                        ExactSemanticVersion.of(SemanticVersion.of(5, 3)),
                        SemanticVersionRange.of(SemanticVersion.of(3), SemanticVersion.of(4)));

        Optional<SemanticVersion> suggested = SemanticVersions.resolve(availableVersions, requirements);

        assertEquals(suggested, Optional.empty());
    }
}
