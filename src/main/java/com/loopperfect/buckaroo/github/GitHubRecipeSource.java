package com.loopperfect.buckaroo.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.tasks.CacheTasks;
import com.loopperfect.buckaroo.tasks.CommonTasks;
import com.loopperfect.buckaroo.tasks.DownloadTask;
import io.reactivex.Single;

import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

public final class GitHubRecipeSource implements RecipeSource {

    private final FileSystem fs;

    private GitHubRecipeSource(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        this.fs = fs;
    }

    private static Single<RecipeVersion> fetchRecipeVersion(final FileSystem fs, final URL release) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(release);

        final Path cachePath = CacheTasks.getCachePath(fs, release, Optional.of("zip"));

        return DownloadTask.download(release, cachePath, true).ignoreElements()
            .andThen(CommonTasks.hash(cachePath))
            .flatMap(fileHashEvent -> {

                final Optional<Path> subPath = EvenMoreFiles.walkZip(cachePath, 1)
                    .skip(1)
                    .findFirst()
                    .map(x -> fs.getPath(fs.getSeparator(), x.toString()));

                final Path unzipTargetPath = CacheTasks.getCacheFolder(fs).resolve(fileHashEvent.sha256.toString());

                return CommonTasks.unzip(cachePath, unzipTargetPath, subPath, StandardCopyOption.REPLACE_EXISTING).flatMap(fileUnzipEvent -> {

                    final Path projectFilePath = fs.getPath(unzipTargetPath.toString(), "buckaroo.json");

                    return CommonTasks.readProjectFile(projectFilePath)
                        .onErrorResumeNext(error -> Single.error(new FetchRecipeException(error)))
                        .map((ReadProjectFileEvent readProjectFileEvent) -> {

                            final RemoteArchive remoteArchive = RemoteArchive.of(
                                release,
                                fileHashEvent.sha256,
                                subPath.map(Path::toString));

                            return RecipeVersion.of(
                                remoteArchive,
                                Optional.empty(),
                                readProjectFileEvent.project.dependencies,
                                Optional.empty());
                        });
                });
            });
    }

    @Override
    public Process<Event, Recipe> fetch(final RecipeIdentifier identifier) {

        Preconditions.checkNotNull(identifier);

        return GitHub.fetchReleases(identifier.organization, identifier.recipe).chain(releases -> {

            final ImmutableMap<SemanticVersion, URL> semanticVersionReleases = releases.entrySet()
                .stream()
                .filter(x -> SemanticVersion.parse(x.getKey()).isPresent())
                .collect(ImmutableMap.toImmutableMap(
                    i -> SemanticVersion.parse(i.getKey()).get(),
                    Map.Entry::getValue));

            if (semanticVersionReleases.isEmpty()) {
                return Process.error(new FetchRecipeException("No releases found for " + identifier.encode() + ". "));
            }

            final ImmutableMap<SemanticVersion, Single<RecipeVersion>> tasks = semanticVersionReleases.entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, x -> fetchRecipeVersion(fs, x.getValue())));

            final Single<ImmutableMap<SemanticVersion, RecipeVersion>> identity = Single.just(ImmutableMap.of());

            final Single<Recipe> task = tasks.entrySet().stream().reduce(
                identity,
                (state, next) -> state.flatMap((ImmutableMap<SemanticVersion, RecipeVersion> map) ->
                    next.getValue().map((RecipeVersion recipeVersion) -> {
                    final SemanticVersion version = next.getKey();
                    return MoreMaps.with(map, version, recipeVersion);
                })),
                (Single<ImmutableMap<SemanticVersion, RecipeVersion>> i, Single<ImmutableMap<SemanticVersion, RecipeVersion>> j) ->
                    i.flatMap(x -> j.map(y -> MoreMaps.merge(x, y))))
                .map((ImmutableMap<SemanticVersion, RecipeVersion> recipeVersions) -> Recipe.of(
                    identifier.recipe.name,
                    "https://github.com/" + identifier.organization + "/" + identifier.recipe,
                    recipeVersions));

            return Process.of(task);
        });
    }

    public static RecipeSource of(final FileSystem fs) {
        return new GitHubRecipeSource(fs);
    }
}
