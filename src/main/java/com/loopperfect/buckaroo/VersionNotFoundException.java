package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;

public final class VersionNotFoundException extends DependencyResolverException {
    private final SemanticVersion version;

    public VersionNotFoundException(final RecipeIdentifier project, final SemanticVersion version) {
        super(project, "Version " + version.encode() + " of " + project.encode() + " not found");
        this.version = Preconditions.checkNotNull(version);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof VersionNotFoundException)) {
            return false;
        }

        final VersionNotFoundException other = (VersionNotFoundException) obj;

        return Objects.equals(id, other.id)
            && Objects.equals(version, other.version);
    }
}