package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;

public final class ResolvedDependency {

    public final Identifier project;
    public final SemanticVersion version;

    private ResolvedDependency(final Identifier project, final SemanticVersion version) {
        this.project = Preconditions.checkNotNull(project);
        this.version = Preconditions.checkNotNull(version);
    }

    public boolean equals(final ResolvedDependency other) {
        return this == other ||
                (Objects.equals(project, other.project) &&
                        Objects.equals(version, other.version));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof ResolvedDependency)) {
            return false;
        }
        return equals((ResolvedDependency) obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, version);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("project", project)
                .add("version", version)
                .toString();
    }

    public static ResolvedDependency of(final Identifier project, final SemanticVersion version) {
        return new ResolvedDependency(project, version);
    }
}
