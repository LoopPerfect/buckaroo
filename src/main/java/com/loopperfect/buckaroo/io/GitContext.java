package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;

import com.loopperfect.buckaroo.Either;
import org.eclipse.jgit.api.*;

import java.io.File;
import java.util.Optional;

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

    static GitContext actual() {
        return new GitContext(){};
    }

    static GitContext fake(){
        return new GitContext() {
            @Override
            public Optional<Exception> clone(final File localPath, final String gitUrl) {
                return Optional.empty();
            }

            @Override
            public Optional<Exception> checkout(final File localPath, final String branch) {
                return Optional.empty();
            }

            @Override
            public Optional<Exception> pull(final File localPath) {
                return Optional.empty();
            }

            @Override
            public Either<Exception, Status> status(final File localPath) {
                return null;
            }
        };
    }
}
