package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class GitCommitHash {

    public final String hash;

    private static final Pattern pattern = Pattern.compile("^([a-f0-9]{5,40})$");

    private GitCommitHash(final String hash) {
        Objects.requireNonNull(hash);
        if (!pattern.matcher(hash).matches()) {
            throw new IllegalArgumentException("Hash must be a valid Git commit hash. Received \"" + hash + "\".  ");
        }
        this.hash = hash;
    }

    public boolean equals(final GitCommitHash other) {
        Objects.requireNonNull(other);
        return Objects.equals(hash, other.hash);
    }

        @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null && obj instanceof GitCommitHash && equals((GitCommitHash)obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .addValue(hash)
            .toString();
    }

    public static GitCommitHash of(final String hash) {
        return new GitCommitHash(hash);
    }

    public static Optional<GitCommitHash> parse(final String hash) {
        if (pattern.matcher(hash).matches()) {
            return Optional.of(new GitCommitHash(hash));
        }
        return Optional.empty();
    }
}
