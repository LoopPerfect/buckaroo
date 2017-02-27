package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;

/**
 * Created by gaetano on 16/02/17.
 */
public final class VersionNotFoundException extends DependencyResolverException {
    private final SemanticVersion version;

    public VersionNotFoundException(final Identifier id, final SemanticVersion ver) {
        super(id, "Version " + ver + "of Project " + id.name + "not found");
        this.version = Preconditions.checkNotNull(ver);
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