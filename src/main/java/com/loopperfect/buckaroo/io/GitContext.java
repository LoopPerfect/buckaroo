package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Either;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.nio.file.Path;

public interface GitContext {

    default Optional<Exception> clone(final File localPath, final String gitUrl) {

        Preconditions.checkNotNull(localPath);
        Preconditions.checkNotNull(gitUrl);

        try {
            final CloneCommand command = Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(localPath);
            command.call();
        } catch (final Exception e) {
            return Optional.of(e);
        }

        return Optional.empty();
    }

    default Optional<Exception> checkout(final File localPath, final String branch) {

        Preconditions.checkNotNull(localPath);
        Preconditions.checkNotNull(branch);

        try {
            final CheckoutCommand command = Git.open(localPath)
                .checkout()
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setName(branch);
            command.call();
        } catch (final Exception e) {
            return Optional.of(e);
        }

        return Optional.empty();
    }

    default Optional<Exception> pull(final File localPath) {

        Preconditions.checkNotNull(localPath);

        try {
            final PullCommand command = Git.open(localPath)
                .pull();
            command.call();
        } catch (final Exception e) {
            return Optional.of(e);
        }

        return Optional.empty();
    }

    default Either<Exception, Status> status(final File localPath) {

        Preconditions.checkNotNull(localPath);

        try {
            final StatusCommand command = Git.open(localPath).status();
            final Status status = command.call();
            return Either.right(status);
        } catch (final Exception e) {
            return Either.left(e);
        }
    }

    default Either<Exception, String> remoteOriginUrl(final Path localPath) {
        Preconditions.checkNotNull(localPath);
        final Path gitDirectory = localPath.getFileSystem().getPath(localPath.toString(), "/", ".git");
        try {
            final Repository repository = new FileRepositoryBuilder()
                .setGitDir(gitDirectory.toFile())
                .build();
            return Either.right(repository.getConfig().getString( "remote", "origin", "url" ));
        } catch (IOException e) {
            return Either.left(e);
        }
    }

    static GitContext actual() {
        return new GitContext() {
        };
    }

    static GitContext fake() {
        return new GitContext() {
            @Override
            public Optional<Exception> clone(final File localPath, final String gitUrl) {
                return Optional.of(new RuntimeException("Not implemented"));
            }

            @Override
            public Optional<Exception> checkout(final File localPath, final String branch) {
                return Optional.of(new RuntimeException("Not implemented"));
            }

            @Override
            public Optional<Exception> pull(final File localPath) {
                return Optional.of(new RuntimeException("Not implemented"));
            }

            @Override
            public Either<Exception, Status> status(final File localPath) {
                return Either.left(new RuntimeException("Not implemented"));
            }
        };
    }
}
