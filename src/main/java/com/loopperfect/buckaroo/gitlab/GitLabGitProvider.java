package com.loopperfect.buckaroo.gitlab;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.GitCommitHash;
import com.loopperfect.buckaroo.GitProvider;
import com.loopperfect.buckaroo.Identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;

public final class GitLabGitProvider implements GitProvider {

    private GitLabGitProvider() {

    }

    @Override
    public Identifier recipeIdentifierPrefix() {
        return Identifier.of("gitlab");
    }

    @Override
    public String gitURL(final Identifier owner, final Identifier project) {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        return "git@gitlab.com:" + owner.name + "/" + project.name + ".git";
    }

    @Override
    public URI projectURL(final Identifier owner, final Identifier project) {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        try {
            return new URI("https://gitlab.com/" + owner.name + "/" + project.name);
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
            return new URI("https://gitlab.com/" + owner.name + "/" + project.name + "/repository/archive.zip?ref=" + commit.hash.substring(0, 40));
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> zipSubPath(final Identifier owner, final Identifier project, final GitCommitHash commit) {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(commit);
        return Optional.of(project.name + "-" + commit.hash + "-" + commit.hash);
    }

    public static GitLabGitProvider of() {
        return new GitLabGitProvider();
    }
}
