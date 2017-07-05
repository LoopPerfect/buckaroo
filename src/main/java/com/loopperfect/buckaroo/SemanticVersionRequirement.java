package com.loopperfect.buckaroo;

public interface SemanticVersionRequirement {

    boolean isSatisfiedBy(final SemanticVersion version);

    String encode();
}
