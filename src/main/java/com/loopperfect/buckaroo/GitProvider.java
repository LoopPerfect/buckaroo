package com.loopperfect.buckaroo;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A GitProvider is a hosted Git service with a notion of owners and projects.
 *
 * For example:
 *  - GitHub
 *  - BitBucketGitProvider
 *  - GitLab
 */
public interface GitProvider {

    Identifier recipeIdentifierPrefix();

    String gitURL(final Identifier owner, final Identifier project);

    URI projectURL(final Identifier owner, final Identifier project);

    URI zipURL(final Identifier owner, final Identifier project, final GitCommitHash commit);

    Optional<Path> zipSubPath(final FileSystem fs, final Identifier owner, final Identifier project, final GitCommitHash commit);
}
