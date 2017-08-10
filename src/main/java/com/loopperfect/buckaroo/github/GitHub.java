package com.loopperfect.buckaroo.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.github.serialization.GitHubSerializer;
import com.loopperfect.buckaroo.tasks.DownloadTask;
import com.loopperfect.buckaroo.tasks.GitTasks;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.StreamSupport;

public final class GitHub {

    private GitHub() {

    }

    public static URI zipURL(final Identifier owner, final Identifier project, final GitCommitHash commit) {

        Objects.requireNonNull(owner);
        Objects.requireNonNull(project);
        Objects.requireNonNull(commit);

        try {
            return new URI("https://github.com/" + owner.name + "/" + project.name + "/archive/" + commit.hash + ".zip");
        } catch (final URISyntaxException e) {
            // Should not happen because we build the URL from sanitized inputs
            throw new IllegalStateException(e);
        }
    }

    public static URL fileURL(final Identifier owner, final Identifier project, final GitCommitHash commit, final Path path) {

        Objects.requireNonNull(owner);
        Objects.requireNonNull(project);
        Objects.requireNonNull(commit);

//        https://raw.githubusercontent.com/njlr/test-lib-tags/138252fac310b976a5ee55ffaa8e9180cf44112b/LICENSE
        final String pathSection = Streams.stream(MorePaths.names(path)).reduce("", (x, y) -> x + "/" + y);

        try {
            return new URL("https://raw.githubusercontent.com/" + owner.name + "/" +
                project.name + "/" + commit.hash + "/" + pathSection);
        } catch (final MalformedURLException e) {
            // Should not happen because we build the URL from sanitized inputs
            throw new IllegalStateException(e);
        }
    }

    public static Process<Event, ImmutableMap<String, GitCommitHash>> fetchReleases(final Identifier owner, final Identifier repo)  {

        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(repo);

        return Process.of(GitTasks.fetchTags("git@github.com:" + owner.name + "/" + repo.name + ".git"));
    }
}
