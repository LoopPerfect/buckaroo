package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Objects;

public final class Organization {

    public final String name;
    public final ImmutableMap<Identifier, Recipe> recipes;

    private Organization(final String name, final ImmutableMap<Identifier, Recipe> recipes) {
        super();
        this.name = Preconditions.checkNotNull(name);
        this.recipes = Preconditions.checkNotNull(recipes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, recipes);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Organization)) {
            return false;
        }
        final Organization other = (Organization) obj;
        return Objects.equals(name, other.name) &&
                Objects.equals(recipes, other.recipes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("recipes", recipes)
                .toString();
    }

    public static Organization of(final String name, final ImmutableMap<Identifier, Recipe> recipes) {
        return new Organization(name, recipes);
    }

    public static Organization of(final String name) {
        return new Organization(name, ImmutableMap.of());
    }
}

