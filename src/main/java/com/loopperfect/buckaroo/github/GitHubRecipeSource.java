package com.loopperfect.buckaroo.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.events.FileDownloadedEvent;
import com.loopperfect.buckaroo.events.FileHashEvent;
import com.loopperfect.buckaroo.events.FileUnzipEvent;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.tasks.CacheTasks;
import com.loopperfect.buckaroo.tasks.CommonTasks;
import com.loopperfect.buckaroo.tasks.DownloadTask;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class GitHubRecipeSource implements RecipeSource {

    private final FileSystem fs;

    private GitHubRecipeSource(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        this.fs = fs;
    }

    private static Process<Event, RecipeVersion> fetchRecipeVersion(
        final FileSystem fs, final Identifier owner, final Identifier project, final GitCommitHash commit) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(commit);

        final URL release = GitHub.zipURL(owner, project, commit);
        final Path cachePath = CacheTasks.getCachePath(fs, release, Optional.of("zip"));

        return Process.concat(

            // 1. Download the release to the cache
            Process.of(
                DownloadTask.download(release, cachePath, true).subscribeOn(Schedulers.io()),
                Single.just(FileDownloadedEvent.of(release, cachePath))),

            Process.chain(

                // 2. Compute the hash
                Process.of(CommonTasks.hash(cachePath).subscribeOn(Schedulers.io())),

                (FileHashEvent fileHashEvent) -> {

                    final FileSystem inMemoryFS = Jimfs.newFileSystem();

                    final Path unzipTargetPath = inMemoryFS.getPath(fileHashEvent.sha256.toString());
                    final Path subPath = fs.getPath(fs.getSeparator(), project.name + "-" + commit.hash);
                    final Path projectFilePath = unzipTargetPath.resolve("buckaroo.json");

                    return Process.chain(

                        // 3. Unzip
                        Process.of(CommonTasks.unzip(cachePath, unzipTargetPath, Optional.of(subPath))
                            .subscribeOn(Schedulers.io())),

                        (FileUnzipEvent fileUnzipEvent) -> Process.chain(

                            // 4. Read the project file
                            Process.of(CommonTasks.readProjectFile(projectFilePath).subscribeOn(Schedulers.io())),

                            // 5. Generate a recipe version from the project file and hash
                            (ReadProjectFileEvent readProjectFileEvent) -> {

                                final RemoteArchive remoteArchive = RemoteArchive.of(
                                    release,
                                    fileHashEvent.sha256,
                                    subPath.toString());

                                return Process.just(RecipeVersion.of(
                                    remoteArchive,
                                    readProjectFileEvent.project.target,
                                    readProjectFileEvent.project.dependencies,
                                    Optional.empty()));
                            }));
                })
        );
    }

    @Override
    public Process<Event, Recipe> fetch(final RecipeIdentifier identifier) {

        Preconditions.checkNotNull(identifier);

        return GitHub.fetchReleases(identifier.organization, identifier.recipe).chain(releases -> {

            final ImmutableMap<SemanticVersion, GitCommitHash> semanticVersionReleases = releases.entrySet()
                .stream()
                .filter(x -> SemanticVersion.parse(x.getKey()).isPresent())
                .collect(ImmutableMap.toImmutableMap(
                    i -> SemanticVersion.parse(i.getKey()).get(),
                    Map.Entry::getValue));

            if (semanticVersionReleases.isEmpty()) {
                return Process.error(new FetchRecipeException("No releases found for " + identifier.encode() + ". "));
            }

            final ImmutableMap<SemanticVersion, Process<Event, RecipeVersion>> tasks = semanticVersionReleases.entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(
                    Map.Entry::getKey,
                    x -> fetchRecipeVersion(fs, identifier.organization, identifier.recipe, x.getValue())));

            final Process<Event, ImmutableMap<SemanticVersion, RecipeVersion>> identity = Process.just(ImmutableMap.of());

            return tasks.entrySet().stream()
                .reduce(
                    identity,
                    (state, next) -> Process.chain(state, map ->
                        next.getValue().map(recipeVersion -> MoreMaps.with(map, next.getKey(), recipeVersion))),
                    (i, j) -> Process.chain(i, x -> j.map(y -> MoreMaps.merge(x, y))))
                .map(recipeVersions -> Recipe.of(
                    identifier.recipe.name,
                    "https://github.com/" + identifier.organization + "/" + identifier.recipe,
                    recipeVersions));
        });
    }

    public static RecipeSource of(final FileSystem fs) {
        return new GitHubRecipeSource(fs);
    }
}
