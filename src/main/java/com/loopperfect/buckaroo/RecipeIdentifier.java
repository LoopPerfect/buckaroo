package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class RecipeIdentifier {

    public final Identifier organization;
    public final Identifier recipe;

    private RecipeIdentifier(final Identifier organization, final Identifier recipe) {
        super();
        this.organization = Preconditions.checkNotNull(organization);
        this.recipe = Preconditions.checkNotNull(recipe);
    }

    public String encode() {
        return organization.name + "/" + recipe.name;
    }

    public boolean equals(final RecipeIdentifier other) {
        Preconditions.checkNotNull(other);
        return this == other || (
                Objects.equals(organization, other.organization) &&
                Objects.equals(recipe, other.recipe));
    }

    @Override
    public int hashCode() {
        return Objects.hash(organization, recipe);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
                !(obj == null || !(obj instanceof RecipeIdentifier)) &&
                        equals((RecipeIdentifier) obj);
    }

    @Override
    public String toString() {
        return organization.name + "/" + recipe.name;
    }

    public static RecipeIdentifier of(final Identifier organization, final Identifier recipe) {
        return new RecipeIdentifier(organization, recipe);
    }

    public static RecipeIdentifier of(final String organization, final String recipe) {
        return new RecipeIdentifier(Identifier.of(organization), Identifier.of(recipe));
    }

    public static Optional<RecipeIdentifier> parse(final String x) {
        Preconditions.checkNotNull(x);
        final String trimmed = x.trim();
        final int pivot = trimmed.indexOf("/");
        if (pivot < 0) {
            return Optional.empty();
        }
        final String org = trimmed.substring(0, pivot);
        if (!Identifier.isValid(org)) {
            return Optional.empty();
        }
        final String project = trimmed.substring(pivot + 1);
        if (!Identifier.isValid(project)) {
            return Optional.empty();
        }
        return Optional.of(RecipeIdentifier.of(Identifier.of(org), Identifier.of(project)));
    }
}
