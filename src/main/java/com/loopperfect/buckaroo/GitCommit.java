package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class GitCommit {

    public final String url;
    public final String commit;

    private GitCommit(final String url, final String commit) {

        Preconditions.checkNotNull(url);
        Preconditions.checkArgument(url.length() > 3);

        Preconditions.checkNotNull(commit);
        Preconditions.checkArgument(commit.length() > 3 && isAlphanumeric(commit));

        this.url = url;
        this.commit = commit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, commit);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof GitCommit)) {
            return false;
        }

        final GitCommit other = (GitCommit) obj;

        return url.equals(other.url) && commit.equals(other.commit);
    }

    @Override
    public String toString() {
        return url + "#" + commit;
    }

    private static boolean isAlphanumeric(final String x) {
        for (int i = 0; i < x.length(); i++) {
            final char c = x.charAt(i);
            if (!Character.isDigit(c) && !Character.isAlphabetic(c)) {
                return false;
            }
        }
        return true;
    }

    public static GitCommit of(final String url, final String commit) {
        return new GitCommit(url, commit);
    }

    public static Optional<GitCommit> parse(final String x) {

        Preconditions.checkNotNull(x);

        final int hashIndex = x.lastIndexOf('#');
        if (hashIndex < 0) {
            return Optional.empty();
        }

        final String url = x.substring(0, hashIndex);

        // TODO: Proper url validation
        if (url.length() < 4) {
            return Optional.empty();
        }

        final String commit = x.substring(hashIndex + 1);

        if (commit.length() < 4 || !isAlphanumeric(commit)) {
            return Optional.empty();
        }

        return Optional.of(GitCommit.of(url, commit));
    }
}
