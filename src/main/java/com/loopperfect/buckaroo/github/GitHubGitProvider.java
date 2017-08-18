package com.loopperfect.buckaroo.github;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.GitCommitHash;
import com.loopperfect.buckaroo.GitProvider;
import com.loopperfect.buckaroo.Identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;

public final class GitHubGitProvider implements GitProvider {

    private GitHubGitProvider() {

    }

    @Override
    public Identifier recipeIdentifierPrefix() {
        return Identifier.of("github");
    }

    @Override
    public String gitURL(final Identifier owner, final Identifier project) {
        return "git@github.com:" + owner.name + "/" + project.name + ".git";
    }

    @Override
    public URI projectURL(final Identifier owner, final Identifier project) {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(project);
        try {
            return new URI("https://github.com/" + owner.name + "/" + project.name);
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URI zipURL(final Identifier owner, final Identifier project, final GitCommitHash commit) {
        try {
            return new URI("https://github.com/" + owner.name + "/" + project.name + "/archive/" + commit.hash.substring(0, 40) + ".zip");
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<String> zipSubPath(final Identifier owner, final Identifier project, final GitCommitHash commit) {
        return Optional.of(project.name + "-" + commit.hash);
    }

    public static GitHubGitProvider of() {
        return new GitHubGitProvider();
    }
}
