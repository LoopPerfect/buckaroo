package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class RecipeIdentifier {

    public final Optional<Identifier> source;
    public final Identifier organization;
    public final Identifier recipe;

    private RecipeIdentifier(final Optional<Identifier> source, final Identifier organization, final Identifier recipe) {
        super();
        this.source = Preconditions.checkNotNull(source);
        this.organization = Preconditions.checkNotNull(organization);
        this.recipe = Preconditions.checkNotNull(recipe);
    }

    public String encode() {
        return source.map(x -> x.name + "+").orElse("") + organization.name + "/" + recipe.name;
    }

    public boolean equals(final RecipeIdentifier other) {
        Preconditions.checkNotNull(other);
        return this == other || (
            Objects.equals(source, other.source) &&
                Objects.equals(organization, other.organization) &&
                Objects.equals(recipe, other.recipe));
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, organization, recipe);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
                !(obj == null || !(obj instanceof RecipeIdentifier)) &&
                        equals((RecipeIdentifier) obj);
    }

    @Override
    public String toString() {
        return encode();
    }

    public static RecipeIdentifier of(final Optional<Identifier> source, final Identifier organization, final Identifier recipe) {
        return new RecipeIdentifier(source, organization, recipe);
    }

    public static RecipeIdentifier of(final Identifier source, final Identifier organization, final Identifier recipe) {
        return new RecipeIdentifier(Optional.of(source), organization, recipe);
    }

    public static RecipeIdentifier of(final Identifier organization, final Identifier recipe) {
        return new RecipeIdentifier(Optional.empty(), organization, recipe);
    }

    public static RecipeIdentifier of(final String source, final String organization, final String recipe) {
        return new RecipeIdentifier(Optional.of(Identifier.of(source)), Identifier.of(organization), Identifier.of(recipe));
    }

    public static RecipeIdentifier of(final String organization, final String recipe) {
        return new RecipeIdentifier(Optional.empty(), Identifier.of(organization), Identifier.of(recipe));
    }

    public static Optional<RecipeIdentifier> parse(final String x) {
        Preconditions.checkNotNull(x);
        final String trimmed = x.trim();
        final int pivot = trimmed.indexOf("/");
        if (pivot < 0) {
            return Optional.empty();
        }
        final String sourceAndOrg = trimmed.substring(0, pivot);
        final int pivot2 = sourceAndOrg.indexOf("+");
        if (pivot2 < 0) {
            if (!Identifier.isValid(sourceAndOrg)) {
                return Optional.empty();
            }
            final String org = sourceAndOrg.trim();
            final String project = trimmed.substring(pivot + 1);
            if (!Identifier.isValid(project)) {
                return Optional.empty();
            }
            return Optional.of(RecipeIdentifier.of(Identifier.of(org), Identifier.of(project)));
        }
        final String source = sourceAndOrg.substring(0, pivot2).trim();
        final String org = sourceAndOrg.substring(pivot2 + 1).trim();
        final String project = trimmed.substring(pivot + 1).trim();
        if (!Identifier.isValid(source)) {
            return Optional.empty();
        }
        if (!Identifier.isValid(org)) {
            return Optional.empty();
        }
        if (!Identifier.isValid(project)) {
            return Optional.empty();
        }
        return Optional.of(RecipeIdentifier.of(Identifier.of(source), Identifier.of(org), Identifier.of(project)));
    }
}
