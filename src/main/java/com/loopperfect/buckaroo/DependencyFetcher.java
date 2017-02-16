package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableMap;

/**
 * Created by gaetano on 16/02/17.
 */

@FunctionalInterface
public interface DependencyFetcher {
    Either<
        ImmutableMap.Entry<Identifier,SemanticVersionRequirement>,
        ImmutableMap<SemanticVersion, Project>> fetch(Identifier id, SemanticVersionRequirement req);
};
