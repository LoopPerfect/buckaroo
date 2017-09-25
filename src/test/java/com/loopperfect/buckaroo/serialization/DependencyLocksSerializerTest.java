package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import org.javatuples.Pair;
import org.junit.Test;

import java.util.Optional;

import static com.loopperfect.buckaroo.Either.left;
import static com.loopperfect.buckaroo.Either.right;
import static org.junit.Assert.assertEquals;

public final class DependencyLocksSerializerTest {

    private static void serializeDeserialize(final DependencyLocks dependencyLocks) {

        final String serializedDependency = Serializers.serialize(dependencyLocks);

        final Either<JsonParseException, DependencyLocks> deserizializedDependency =
            Serializers.parseDependencyLocks(serializedDependency);

        assertEquals(right(dependencyLocks), deserizializedDependency);
    }

    @Test
    public void test1() throws Exception {

        serializeDeserialize(DependencyLocks.of(ImmutableMap.of(
            RecipeIdentifier.of("org", "project"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/project/commit", "b0215d5")),
                ImmutableList.of(
                    RecipeIdentifier.of("org", "project"))))));
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
                    RecipeIdentifier.of("megacorp", "json"))),
            RecipeIdentifier.of("org", "anotherproject"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/anotherproject/commit", "b0215d5")),
                ImmutableList.of(
                    RecipeIdentifier.of("megacorp", "threads"))))));
    }

    @Test
    public void test3() throws Exception {

        serializeDeserialize(DependencyLocks.of(ImmutableMap.of(
            RecipeIdentifier.of("github", "org", "project"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/project/commit", "b0215d5")),
                ImmutableList.of()))));
    }

    @Test
    public void test4() throws Exception {

        serializeDeserialize(DependencyLocks.of(ImmutableMap.of(
            RecipeIdentifier.of("github", "org", "project"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/project/commit", "b0215d5")),
                Optional.empty(),
                Optional.empty(),
                ImmutableList.of(),
                ImmutableList.of()))));
    }

    @Test
    public void test5() throws Exception {

        serializeDeserialize(DependencyLocks.of(ImmutableMap.of(
            RecipeIdentifier.of("github", "org", "project"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/project/commit", "b0215d5")),
                Optional.empty(),
                Optional.empty(),
                ImmutableList.of(),
                ImmutableList.of(
                    ResolvedPlatformDependencies.of(
                        "linux.*",
                        ImmutableList.of(RecipeIdentifier.of("org", "example"))))))));
    }

    @Test
    public void test6() throws Exception {

        final String serialized = "{\n" +
            "  \"github+org/project\": {\n" +
            "    \"source\": \"https://github.com/org/project/commit#b0215d5\",\n" +
            "    \"dependencies\": [\n" +
            "      \"org/someexample\"\n" +
            "    ],\n" +
            "    \"platformDependencies\": [\n" +
            "      {\n" +
            "        \"platform\": \"linux.*\",\n" +
            "        \"dependencies\": [\n" +
            "          \"org/anotherexample\"\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        final DependencyLocks deserialized = DependencyLocks.of(ImmutableMap.of(
            RecipeIdentifier.of("github", "org", "project"),
            ResolvedDependency.of(
                Either.left(GitCommit.of("https://github.com/org/project/commit", "b0215d5")),
                Optional.empty(),
                Optional.empty(),
                ImmutableList.of(RecipeIdentifier.of("org", "someexample")),
                ImmutableList.of(
                    ResolvedPlatformDependencies.of(
                        "linux.*",
                        ImmutableList.of(RecipeIdentifier.of("org", "anotherexample")))))));

        assertEquals(right(deserialized), Serializers.parseDependencyLocks(serialized));
    }

    @Test
    public void test7() {

        final DependencyLocks dependencyLocks = DependencyLocks.of(
            ImmutableList.of(
                RecipeIdentifier.of("org", "example")),
            ImmutableList.of(
                ResolvedPlatformDependencies.of(
                    "^linux.*",
                    ImmutableList.of(
                        RecipeIdentifier.of("org", "linux-only")))),
            ImmutableMap.of(
                RecipeIdentifier.of("org", "example"),
                ResolvedDependency.of(left(GitCommit.of("https://github.com/org/example/commit", "c7355d5"))),
                RecipeIdentifier.of("org", "linux-only"),
                ResolvedDependency.of(left(GitCommit.of("https://github.com/org/linux-only/commit", "b8945e7")))));

        assertEquals(
            right(dependencyLocks),
            Serializers.parseDependencyLocks(Serializers.serialize(dependencyLocks)));
    }
}
