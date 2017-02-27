package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableSet;

public interface SemanticVersionRequirement {

    boolean isSatisfiedBy(final SemanticVersion version);

    ImmutableSet<SemanticVersion> hints();

    String encode();
}
