package com.loopperfect.buckaroo.resolver;

import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.ResolvedDependency;
import com.loopperfect.buckaroo.SemanticVersion;
import org.javatuples.Pair;

import java.util.Map;

@FunctionalInterface
public interface ResolutionStrategy {

    int score(final Map<RecipeIdentifier, Pair<SemanticVersion, ResolvedDependency>> resolvedDependencies);
}
