package com.loopperfect.buckaroo.github;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.github.serialization.GitHubSerializer;
import com.loopperfect.buckaroo.tasks.DownloadTask;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.StreamSupport;

public final class GitHub {

    private GitHub() {

    }

    public static Process<Event, ImmutableList<GitHubRelease>> fetchReleaseNames(final Identifier owner, final Identifier repo)  {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(repo);
        final String urlStr = "https://api.github.com/repos/" + owner.name + "/" + repo.name + "/releases";
        Process<Event, URL> u = Process.of(
            Observable.just(urlStr)
                .map(URL::new)
                .map(url-> {
                    final Either<Event, URL> e = Either.right(url);
                    return e;
            })
        );

        return u.chain(DownloadTask::download)
            .chain(content -> {
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
