package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Objects;

public final class Cookbook {

    public final ImmutableMap<Identifier, Organization> organizations;

    private Cookbook(final ImmutableMap<Identifier, Organization> organizations) {
        this.organizations = Preconditions.checkNotNull(organizations);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Cookbook)) {
            return false;
        }
        final Cookbook other = (Cookbook) obj;
        return Objects.equals(organizations, other.organizations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizations);
    }

    public static Cookbook of(final ImmutableMap<Identifier, Organization> organizations) {
        return new Cookbook(organizations);
    }
}
