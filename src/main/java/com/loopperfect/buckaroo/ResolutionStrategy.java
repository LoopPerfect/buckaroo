package com.loopperfect.buckaroo;

import java.util.Map;

@FunctionalInterface
public interface ResolutionStrategy {

    int score(final Map<RecipeIdentifier, SemanticVersion> resolvedDependencies);
}
