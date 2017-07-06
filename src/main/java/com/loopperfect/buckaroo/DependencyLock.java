package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;

public final class DependencyLock {

    public final RecipeIdentifier identifier;
    public final ResolvedDependency origin;

    private DependencyLock(final RecipeIdentifier identifier, final ResolvedDependency origin) {
        super();

        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(origin);

        this.identifier = identifier;
        this.origin = origin;
    }

    public boolean equals(final DependencyLock other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(identifier, other.identifier) &&
            Objects.equals(origin, other.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, origin);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof DependencyLock &&
            equals((DependencyLock) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("identifier", identifier)
            .add("origin", origin)
            .toString();
    }

    public static DependencyLock of(final RecipeIdentifier identifier, final ResolvedDependency origin) {
        return new DependencyLock(identifier, origin);
    }
}
