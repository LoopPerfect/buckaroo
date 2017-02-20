package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;

/**
 * Created by gaetano on 16/02/17.
 */
public final class VersionNotFoundException extends DependencyResolverException {
    public final Identifier id;
    public final SemanticVersion ver;

    public VersionNotFoundException(final Identifier id, final SemanticVersion ver){
        super("Version " + ver + "of Project "+ id.name + "not found");
        this.id = Preconditions.checkNotNull(id);
        this.ver = Preconditions.checkNotNull(ver);
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
            && Objects.equals(ver, other.ver);
    }
}