package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;

@Deprecated
public final class GitHubIdentifier implements DependencyIdentifier {

    public final String user;
    public final String project;

    private GitHubIdentifier(final String user, final String project) {

        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(project);

        this.user = user;
        this.project = project;
    }

    public boolean equals(final GitHubIdentifier other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(user, other.user) &&
            Objects.equals(project, other.project);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof GitHubIdentifier &&
            equals((GitHubIdentifier)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, project);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("user", user)
            .add("project", project)
            .toString();
    }

    public GitHubIdentifier of(final String user, final String project) {
        return new GitHubIdentifier(user, project);
    }
}
