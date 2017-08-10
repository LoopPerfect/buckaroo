package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.GitCommit;
import com.loopperfect.buckaroo.GitCommitHash;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.events.DeleteFileEvent;
import com.loopperfect.buckaroo.events.GitCheckoutEvent;
import com.loopperfect.buckaroo.events.GitCloneEvent;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public final class GitTasks {

    private GitTasks() {

    }

    public static Observable<Event> ensureCloneAndCheckout(final GitCommit commit, final Path directory, final boolean overwrite) {
        Preconditions.checkNotNull(commit);
        Preconditions.checkNotNull(directory);
        return Observable.create(emitter -> {
            try {
                // Is there a folder there already?
                if (Files.exists(directory)) {
                    // Check the status.
                    final Git git = Git.open(directory.toFile());
                    final Status status = git.status().call();
                    final String remote = git.getRepository()
                        .getConfig()
                        .getString("remote", "origin", "url");
                    final boolean isValidStatus = status.isClean() && remote.equalsIgnoreCase(commit.url);
                    // Is it invalid?
                    if (!isValidStatus) {
                        // Can we destroy files?
                        if (overwrite) {
                            // Delete what we have.
                            Files.deleteIfExists(directory);
                            emitter.onNext(DeleteFileEvent.of(directory));
                        } else {
                            throw new FileAlreadyExistsException(directory.toString());
                        }
                    }
                } else {
                    // Clone!
                    Git.cloneRepository()
                        .setURI(commit.url)
                        .setDirectory(directory.toFile())
                        .call();
                    emitter.onNext(GitCloneEvent.of(commit.url, directory));
                }
                // Checkout
                Git.open(directory.toFile())
                    .checkout()
                    .setName(commit.commit)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call();
                emitter.onNext(GitCheckoutEvent.of(directory, commit.commit));
                emitter.onComplete();
            } catch (final Throwable e) {
                emitter.onError(e);
            }
        });
    }

    public static Single<ImmutableMap<String, GitCommitHash>> fetchTags(final String gitURL) {

        Preconditions.checkNotNull(gitURL);

        return Single.fromCallable(() -> {

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
        });
    }
}
