package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class PartialRecipeIdentifier {

    public final Optional<Identifier> source;
    public final Optional<Identifier> organization;
    public final Identifier project;

    private PartialRecipeIdentifier(
        final Optional<Identifier> source,
        final Optional<Identifier> organization,
        final Identifier project) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(organization);
        Preconditions.checkNotNull(project);

        this.source = source;
        this.organization = organization;
        this.project = project;
    }

    public boolean equals(final PartialRecipeIdentifier other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(source, other.source) &&
            Objects.equals(organization, other.organization) &&
            Objects.equals(project, other.project);
    }

    public String encode() {
        return source.map(x -> x + "+").orElse("") +
            organization.map(x -> x + "/").orElse("") +
            project.name;
    }

    public boolean isSatisfiedBy(final RecipeIdentifier identifier) {

        Preconditions.checkNotNull(identifier);

        return source.map(x -> identifier.source.map(x::equals).orElse(false)).orElse(true) &&
            organization.map(identifier.organization::equals).orElse(true) &&
            project.equals(identifier.recipe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, organization, project);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null &&
                obj instanceof PartialRecipeIdentifier &&
                equals((PartialRecipeIdentifier) obj);
    }

    @Override
    public String toString() {
        return encode();
    }

    public static PartialRecipeIdentifier of(
        final Optional<Identifier> source,
        final Optional<Identifier> organization,
        final Identifier project) {
        return new PartialRecipeIdentifier(
            source,
            organization,
            project);
    }

    public static PartialRecipeIdentifier of(
        final Identifier source,
        final Identifier organization,
        final Identifier project) {
        return new PartialRecipeIdentifier(
            Optional.of(source),
            Optional.of(organization),
            project);
    }

    public static PartialRecipeIdentifier of(
        final Identifier organization,
        final Identifier project) {
        return new PartialRecipeIdentifier(
            Optional.empty(),
            Optional.of(organization),
            project);
    }

    public static PartialRecipeIdentifier of(
        final RecipeIdentifier recipeIdentifier) {
        return new PartialRecipeIdentifier(
            recipeIdentifier.source,
            Optional.of(recipeIdentifier.organization),
            recipeIdentifier.recipe);
    }

    public static PartialRecipeIdentifier of(final Identifier project) {
        return new PartialRecipeIdentifier(
            Optional.empty(),
            Optional.empty(),
            project);
    }
}
