package com.loopperfect.buckaroo.resolver;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.ResolvedDependencies;
import com.loopperfect.buckaroo.ResolvedDependency;
import com.loopperfect.buckaroo.SemanticVersion;
import org.javatuples.Pair;

import java.util.Map;

public final class SumResolutionStrategy implements ResolutionStrategy {

    private SumResolutionStrategy() {

    }

    @Override
    public int hashCode() {
        return  0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof SumResolutionStrategy;
    }

    private static int score(final SemanticVersion version) {
        Preconditions.checkNotNull(version);
        return version.major * 100 * 100 + version.minor * 100 + version.patch;
    }

    @Override
    public int score(final ResolvedDependencies resolvedDependencies) {
        Preconditions.checkNotNull(resolvedDependencies);
        return resolvedDependencies.dependencies.values().stream()
            .map(Pair::getValue0)
            .mapToInt(SumResolutionStrategy::score).sum();
    }

    public static ResolutionStrategy of() {
        return new SumResolutionStrategy();
    }
}
