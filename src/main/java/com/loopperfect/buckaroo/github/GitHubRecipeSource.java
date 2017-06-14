package com.loopperfect.buckaroo.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.tasks.CacheTasks;
import com.loopperfect.buckaroo.tasks.CommonTasks;
import com.loopperfect.buckaroo.tasks.DownloadTask;
import io.reactivex.Single;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class GitHubRecipeSource implements RecipeSource {

    private final FileSystem fs;

    private GitHubRecipeSource(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        this.fs = fs;
    }

    private static Single<RecipeVersion> fetchRecipeVersion(final FileSystem fs, final GitHubRelease release) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(release);

        final Path cachePath = CacheTasks.getCachePath(fs, release.zipURL, Optional.of("zip"));

        return DownloadTask.download(release.zipURL, cachePath, true).ignoreElements()
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
                                release.zipURL,
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

        return GitHub.fetchReleaseNames(identifier.organization, identifier.recipe).chain(
            releases -> {
            final ImmutableList<GitHubRelease> semanticVersionReleases = releases.stream()
                .filter(x -> SemanticVersion.parse(x.name).isPresent())
                .collect(ImmutableList.toImmutableList());

            if (semanticVersionReleases.isEmpty()) {
                return Process.error(new FetchRecipeException("No releases found for " + identifier.encode() + ". "));
            }

            final ImmutableMap<GitHubRelease, Single<RecipeVersion>> tasks = semanticVersionReleases.stream()
                .collect(ImmutableMap.toImmutableMap(x -> x, x -> fetchRecipeVersion(fs, x)));

            final Single<ImmutableMap<SemanticVersion, RecipeVersion>> identity = Single.just(ImmutableMap.of());

            final Single<Recipe> xxx = tasks.entrySet().stream().reduce(
                identity,
                (state, next) -> state.flatMap(map -> next.getValue().map(recipeVersion -> {
                    final SemanticVersion version = SemanticVersion.parse(next.getKey().name).get();
                    return MoreMaps.with(map, version, recipeVersion);
                })),
                (i, j) -> i.flatMap(x -> j.map(y -> MoreMaps.merge(x, y))))
                .map((ImmutableMap<SemanticVersion, RecipeVersion> recipeVersions) -> Recipe.of(
                    identifier.recipe.name,
                    "https://github.com/" + identifier.organization + "/" + identifier.recipe,
                    recipeVersions));

            return Process.of(xxx);
        });
    }

    public static RecipeSource of(final FileSystem fs) {
        return new GitHubRecipeSource(fs);
    }
}
