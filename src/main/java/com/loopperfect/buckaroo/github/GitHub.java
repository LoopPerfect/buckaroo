package com.loopperfect.buckaroo.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.github.serialization.GitHubSerializer;
import com.loopperfect.buckaroo.tasks.DownloadTask;
import io.reactivex.Single;

import java.net.URL;
import java.util.stream.StreamSupport;

public final class GitHub {

    private GitHub() {

    }

    public static Single<ImmutableList<GitHubRelease>> fetchReleaseNames(final Identifier owner, final Identifier repo) {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(repo);
        return Single.fromCallable(() ->
            new URL("https://api.github.com/repos/" + owner.name + "/" + repo.name + "/releases"))
            .flatMap(DownloadTask::download)
            .map(content -> {
                final JsonArray elements = new JsonParser().parse(content).getAsJsonArray();
                return StreamSupport.stream(elements.spliterator(), false)
                    .map(GitHubSerializer::parseGitHubRelease)
                    .filter(x -> x.right().isPresent())
                    .map(x -> x.right().get())
                    .collect(ImmutableList.toImmutableList());
            });
    }
}
