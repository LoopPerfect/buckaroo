package com.loopperfect.buckaroo.bitbucket;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.GitCommitHash;
import com.loopperfect.buckaroo.GitProvider;
import com.loopperfect.buckaroo.Identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;

public final class BitBucketGitProvider implements GitProvider {

    private BitBucketGitProvider() {

    }

    @Override
    public Identifier recipeIdentifierPrefix() {
        return Identifier.of("bitbucket");
    }

    @Override
    public String gitURL(final Identifier owner, final Identifier project) {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        return "git@bitbucket.org:" + owner.name + "/" + project.name + ".git";
    }

    @Override
    public URI projectURL(final Identifier owner, final Identifier project) {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        try {
            return new URI("https://bitbucket.org/" + owner.name + "/" + project.name);
        } catch (final URISyntaxException e) {
            // Should not happen because identifiers are validated in their constructor.
            throw new RuntimeException(e);
        }
    }

    @Override
    public URI zipURL(final Identifier owner, final Identifier project, final GitCommitHash commit) {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(commit);
        try {
            return new URI("https://bitbucket.org/" + owner.name + "/" + project.name + "/get/" + commit.hash.substring(0, 12) + ".zip");
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Path> zipSubPath(final FileSystem fs, final Identifier owner, final Identifier project, final GitCommitHash commit) {
        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(commit);
        return Optional.of(fs.getPath(fs.getSeparator(),owner.name + "-" + project.name + "-" + commit.hash.substring(0, 12)));
    }

    public static BitBucketGitProvider of() {
        return new BitBucketGitProvider();
    }
}
