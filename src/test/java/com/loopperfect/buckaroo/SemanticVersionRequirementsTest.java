package com.loopperfect.buckaroo;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public final class SemanticVersionRequirementsTest {

    @org.junit.Test
    public void testParse() {
        assertEquals(
                Optional.of(ExactSemanticVersion.of(SemanticVersion.of(1, 2, 3))),
                SemanticVersionRequirements.parse("1.2.3"));

        assertEquals(
                Optional.empty(),
                SemanticVersionRequirements.parse("thisisnotvalid"));
    }
}
