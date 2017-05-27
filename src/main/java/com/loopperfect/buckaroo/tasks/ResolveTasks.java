package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.AsyncDependencyResolver;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RecipeSource;
import com.loopperfect.buckaroo.sources.GitHubRecipeSource;
import com.loopperfect.buckaroo.sources.LazyCookbookRecipeSource;
import com.loopperfect.buckaroo.sources.RecipeSources;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.nio.file.Path;

public final class ResolveTasks {

    private ResolveTasks() {

    }

    public static Observable<?> resolveDependenciesInWorkingDirectory(final FileSystem fs) {
        Preconditions.checkNotNull(fs);
        final Path projectFilePath = fs.getPath("buckaroo.json").toAbsolutePath();
        return CommonTasks.readAndMaybeGenerateConfigFile(fs).toObservable().flatMap(
            config -> {
                return CommonTasks.readProjectFile(projectFilePath).toObservable().flatMap(
                    project -> {

                        final Path cookbookPath = fs.getPath(
                            System.getProperty("user.home"),
                            ".buckaroo",
                            config.cookbooks.get(0).name.name);

                        final RecipeSource recipeSource = RecipeSources.routed(
                            ImmutableMap.of(
                                Identifier.of("github"), GitHubRecipeSource.of()),
                            LazyCookbookRecipeSource.of(cookbookPath));

                        return AsyncDependencyResolver.resolve(
                            recipeSource, project.dependencies.entries()).toObservable();
                    });
            });
    }
}
