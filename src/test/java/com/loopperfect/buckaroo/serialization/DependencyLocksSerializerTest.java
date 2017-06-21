package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public final class DependencyLocksSerializerTest {

    private static void serializeDeserialize(final DependencyLocks dependencyLocks) {

        final String serializedDependency = Serializers.serialize(dependencyLocks);

        final Either<JsonParseException, DependencyLocks> deserizializedDependency =
            Serializers.parseDependencyLocks(serializedDependency);

        assertEquals(Either.right(dependencyLocks), deserizializedDependency);
    }

    @Test
    public void test1() throws Exception {

        serializeDeserialize(DependencyLocks.of(ImmutableMap.of(
            RecipeIdentifier.of("org", "project"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/project/commit", "b0215d5")),
                ImmutableList.of(
                    ResolvedDependencyReference.of(RecipeIdentifier.of("org", "project")))))));
    }

    @Test
    public void test2() throws Exception {

        serializeDeserialize(DependencyLocks.of(ImmutableMap.of(
            RecipeIdentifier.of("org", "project"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/project/commit", "b0215d5")),
                Optional.of("project-lib"),
                Optional.empty(),
                ImmutableList.of(
                    ResolvedDependencyReference.of(RecipeIdentifier.of("megacorp", "json"), "json-lib"))),
            RecipeIdentifier.of("org", "anotherproject"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/anotherproject/commit", "b0215d5")),
                ImmutableList.of(
                    ResolvedDependencyReference.of(RecipeIdentifier.of("megacorp", "threads")))))));
    }

    @Test
    public void test3() throws Exception {

        serializeDeserialize(DependencyLocks.of(ImmutableMap.of(
            RecipeIdentifier.of("github", "org", "project"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/project/commit", "b0215d5")),
                ImmutableList.of()))));
    }
}
