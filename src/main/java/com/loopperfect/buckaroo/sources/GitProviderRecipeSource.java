package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.events.FetchRecipeProgressEvent;
import com.loopperfect.buckaroo.events.FileDownloadedEvent;
import com.loopperfect.buckaroo.events.FileHashEvent;
import com.loopperfect.buckaroo.events.FileUnzipEvent;
import com.loopperfect.buckaroo.tasks.CacheTasks;
import com.loopperfect.buckaroo.tasks.CommonTasks;
import com.loopperfect.buckaroo.tasks.DownloadTask;
import com.loopperfect.buckaroo.tasks.GitTasks;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class GitProviderRecipeSource implements RecipeSource {

    private final FileSystem fs;
    private final GitProvider gitProvider;

    private GitProviderRecipeSource(final FileSystem fs, final GitProvider gitProvider) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(gitProvider);

        this.fs = fs;
        this.gitProvider = gitProvider;
    }

    private Process<Event, RecipeVersion> fetchRecipeVersion(
        final FileSystem fs, final Identifier owner, final Identifier project, final GitCommitHash commit) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(commit);

        final URI release = gitProvider.zipURL(owner, project, commit);
        final Path cachePath = CacheTasks.getCachePath(fs, release, Optional.of("zip"));

        return Process.concat(

            // 1. Download the release to the cache
            Process.of(
                Observable.combineLatest(
                    Observable.just(RecipeIdentifier.of(gitProvider.recipeIdentifierPrefix(), owner, project)),
                    DownloadTask.download(release, cachePath, true),
                    FetchRecipeProgressEvent::of),
                Single.just(FileDownloadedEvent.of(release, cachePath))),

            Process.chain(

                // 2. Compute the hash
                Process.of(CommonTasks.hash(cachePath)),

                (FileHashEvent fileHashEvent) -> {

                    final FileSystem inMemoryFS = Jimfs.newFileSystem();

                    final Path unzipTargetPath = inMemoryFS.getPath(fileHashEvent.sha256.toString());
                    final Optional<String> subPath = gitProvider.zipSubPath(owner, project, commit);
                    final Path projectFilePath = unzipTargetPath.resolve("buckaroo.json");

                    return Process.chain(

                        // 3. Unzip
                        Process.of(CommonTasks.unzip(cachePath, unzipTargetPath, subPath)),

                        (FileUnzipEvent fileUnzipEvent) -> Process.chain(

                            // 4. Read the project file
                            CommonTasks.readProjectFile(projectFilePath),

                            // 5. Generate a recipe version from the project file and hash
                            (Project p) -> {

                                final RemoteArchive remoteArchive = RemoteArchive.of(
                                    release,
                                    fileHashEvent.sha256,
                                    subPath);

                                return Process.just(RecipeVersion.of(
                                    remoteArchive,
                                    p.target,
                                    p.dependencies,
                                    Optional.empty()));
                            }));
                })
        );
    }

    @Override
    public Process<Event, Recipe> fetch(final RecipeIdentifier identifier) {

        Preconditions.checkNotNull(identifier);

        return Process.of(GitTasks.fetchTags(gitProvider.gitURL(identifier.organization, identifier.recipe)), Event.class)
            .chain(tags -> {

                final ImmutableMap<SemanticVersion, GitCommitHash> semanticVersionReleases = tags.entrySet()
                    .stream()
                    .filter(x -> SemanticVersion.parse(x.getKey()).isPresent())
                    .collect(ImmutableMap.toImmutableMap(
                        i -> SemanticVersion.parse(i.getKey()).get(),
                        Map.Entry::getValue));

                if (semanticVersionReleases.isEmpty()) {
                    return Process.error(new RecipeFetchException(
                        this, identifier, "No tags found for " + identifier.encode() + ". "));
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
                            next.getValue()
                                // This nifty section is where we skip over any errors!
                                // We map every success to an optional, and
                                // then every error is mapped to an empty optional.
                                .map(Optional::of)
                                .onErrorReturn(throwable -> Optional.empty())
                                .map(maybeRecipeVersion -> maybeRecipeVersion
                                    .map(recipeVersion -> MoreMaps.with(map, next.getKey(), recipeVersion)).orElse(map))),
                        (i, j) -> Process.chain(i, x -> j.map(y -> MoreMaps.merge(x, y))))
                    .map(recipeVersions -> Recipe.of(
                        identifier.recipe.name,
                        gitProvider.projectURL(identifier.organization, identifier.recipe),
                        recipeVersions));
            })
            .mapErrors(error -> RecipeFetchException.wrap(this, identifier, error));
    }

    public static RecipeSource of(final FileSystem fs, final GitProvider gitProvider) {
        return new GitProviderRecipeSource(fs, gitProvider);
    }
}
