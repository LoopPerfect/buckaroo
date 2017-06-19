package com.loopperfect.buckaroo.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.github.serialization.GitHubSerializer;
import com.loopperfect.buckaroo.tasks.DownloadTask;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

public final class GitHub {

    private GitHub() {

    }

    public static URL zipURL(final Identifier owner, final Identifier project, final GitCommitHash commit) {

        Objects.requireNonNull(owner);
        Objects.requireNonNull(project);
        Objects.requireNonNull(commit);

        try {
            return new URL("https://github.com/" + owner.name + "/" + project.name + "/archive/" + commit.hash + ".zip");
        } catch (final MalformedURLException e) {
            // Should not happen because we build the URL from sanitized inputs
            throw new IllegalStateException(e);
        }
    }

    public static ImmutableMap<String, GitCommitHash> fetchTags(final Identifier owner, final Identifier repo) throws GitAPIException, IOException {

        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(repo);

        final String gitURL = "git@github.com:" + owner.name + "/" + repo.name + ".git";

        // The repository is not actually used, JGit just seems to require it.
        final Repository repository = FileRepositoryBuilder.create(Paths.get("").toFile());
        final Collection<Ref> refs = new LsRemoteCommand(repository)
            .setRemote(gitURL)
            .setTags(true)
            .call();

        final String prefix = "refs/tags/";

        return refs.stream()
            .filter(x -> x.getTarget().getName().startsWith(prefix))
            .collect(ImmutableMap.toImmutableMap(
                x -> x.getTarget().getName().substring(prefix.length()),
                x -> GitCommitHash.of(x.getObjectId().getName())));
    }

    public static Process<Event, ImmutableMap<String, URL>> fetchReleases(final Identifier owner, final Identifier repo)  {

        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(repo);

        return Process.of(Single.fromCallable(() -> fetchTags(owner, repo))
            .map(x -> x.entrySet()
                .stream()
                .collect(ImmutableMap.toImmutableMap(
                    Map.Entry::getKey,
                    i -> zipURL(owner, repo, i.getValue())))));
    }

    // We are not using this because the GitHub API limits are quite strict without an access token.
    // Whilst using an access token isn't too hard to implement, it adds friction for first-time users.
    public static Process<Event, ImmutableList<GitHubRelease>> fetchReleasesUsingAPI(final Identifier owner, final Identifier repo)  {

        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(repo);

        final String rawURL = "https://api.github.com/repos/" + owner.name + "/" + repo.name + "/releases";

        Process<Event, URL> process = Process.of(
            Observable.just(rawURL)
                .map(URL::new)
                .map(Either::right));

        return process.chain(DownloadTask::download).chain(content -> {
            final JsonArray elements = new JsonParser().parse(content).getAsJsonArray();
            final ImmutableList<GitHubRelease> releases = StreamSupport.stream(elements.spliterator(), false)
                .map(GitHubSerializer::parseGitHubRelease)
                .filter(x -> x.right().isPresent())
                .map(x -> x.right().get())
                .collect(ImmutableList.toImmutableList());
            return Process.of(Single.just(releases));
        });
    }
}
