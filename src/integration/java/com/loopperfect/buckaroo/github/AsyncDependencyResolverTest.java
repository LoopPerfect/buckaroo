package com.loopperfect.buckaroo.github;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.github.GitHubRecipeSource;
import com.loopperfect.buckaroo.resolver.AsyncDependencyResolver;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import io.reactivex.Single;
import org.javatuples.Pair;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public final class AsyncDependencyResolverTest {

    @Test
    public void resolverWorksWithGitHub() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final RecipeSource recipeSource = GitHubRecipeSource.of(fs);

        final Single<ImmutableMap<RecipeIdentifier, Pair<SemanticVersion, ResolvedDependency>>> task = AsyncDependencyResolver.resolve(
            recipeSource,
            ImmutableList.of(Dependency.of(RecipeIdentifier.of("github", "njlr", "test-lib-a"), AnySemanticVersion.of())))
            .result();

        final ImmutableMap<RecipeIdentifier, Pair<SemanticVersion, ResolvedDependency>> resolved =
            task.timeout(120, TimeUnit.SECONDS).blockingGet();

        assertTrue(resolved.containsKey(RecipeIdentifier.of("github", "njlr", "test-lib-a")));
        assertTrue(resolved.containsKey(RecipeIdentifier.of("github", "njlr", "test-lib-b")));
        assertTrue(resolved.containsKey(RecipeIdentifier.of("github", "njlr", "test-lib-c")));
    }
}
