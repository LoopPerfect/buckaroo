package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.GitCommit;
import io.reactivex.Completable;
import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GitTasks {

    private GitTasks() {

    }

    public static Completable clone(final GitCommit commit, final Path directory) {
        Preconditions.checkNotNull(commit);
        Preconditions.checkNotNull(directory);
        return Completable.fromAction(() -> {
            if (!Files.exists(directory)) {
                throw new IOException("Clone directory " + directory.toString() + " does not exist. ");
            }
            if (!Files.isDirectory(directory)) {
                throw new IOException(directory.toString() + " is not a directory. ");
            }
            Git.cloneRepository()
                .setRemote(commit.commit)
                .setDirectory(directory.toFile())
                .call();
        });
    }

    public static Completable ensureCloneAndCheckout(final GitCommit commit, final Path directory) {
        Preconditions.checkNotNull(commit);
        Preconditions.checkNotNull(directory);
        return Completable.fromAction(() -> {

            if (Files.exists(directory)) {

            }
            // TODO
        });
    }
}
