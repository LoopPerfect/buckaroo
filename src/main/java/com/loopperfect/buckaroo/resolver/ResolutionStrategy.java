package com.loopperfect.buckaroo.resolver;

import com.loopperfect.buckaroo.ResolvedDependencies;

@FunctionalInterface
public interface ResolutionStrategy {

    int score(final ResolvedDependencies resolvedDependencies);
}
