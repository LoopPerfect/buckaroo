package com.loopperfect.buckaroo.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.versioning.ExactSemanticVersion;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public final class InstallTasksUnitTest {

    @Test
    public void completeDependencies() throws Exception {

        final RecipeSource recipeSource = recipeIdentifier -> {
            if (recipeIdentifier.equals(RecipeIdentifier.of("org", "example"))) {
                return Process.just(Recipe.of(
                    "example",
                    "https://github.com/org/example",
                    ImmutableMap.of(
                        SemanticVersion.of(1),
                        RecipeVersion.of(
                            GitCommit.of("https://github.com/org/example/commit", "c7355d5"),
                            "example"),
                        SemanticVersion.of(2),
                        RecipeVersion.of(
                            GitCommit.of("https://github.com/org/example/commit", "d8255g5"),
                            "example"))));
            }
            return Process.error(new IOException(recipeIdentifier + " could not be found. "));
        };

        final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
            PartialDependency.of(Identifier.of("org"), Identifier.of("example")));

        final ImmutableList<Dependency> actual = InstallTasks.completeDependencies(recipeSource, partialDependencies)
            .result()
            .blockingGet();

        final ImmutableList<Dependency> expected = ImmutableList.of(
            Dependency.of(RecipeIdentifier.of("org", "example"), ExactSemanticVersion.of(SemanticVersion.of(2))));

        assertEquals(expected, actual);
    }
}
