package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Optional;

public final class PartialDependency {

    public final Optional<Identifier> source;
    public final Optional<Identifier> organization;
    public final Identifier project;
    public final Optional<SemanticVersionRequirement> versionRequirement;

    private PartialDependency(
        final Optional<Identifier> source,
        final Optional<Identifier> organization,
        final Identifier project,
        final Optional<SemanticVersionRequirement> versionRequirement) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(organization);
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(versionRequirement);

        this.source = source;
        this.organization = organization;
        this.project = project;
        this.versionRequirement = versionRequirement;
    }

    public boolean equals(final PartialDependency other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(source, other.source) &&
            Objects.equals(organization, other.organization) &&
            Objects.equals(project, other.project) &&
            Objects.equals(versionRequirement, other.versionRequirement);
    }

    public String encode() {
        return source.map(x -> x + "+").orElse("") +
            (organization.isPresent() ? organization.get().name + "/"  : "") +
            project.name +
            versionRequirement.map(x -> " " + x.encode()).orElse("");
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, organization, project, versionRequirement);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null &&
                obj instanceof PartialDependency &&
                equals((PartialDependency) obj);
    }

    @Override
    public String toString() {
        return encode();
    }

    public static PartialDependency of(
        final Optional<Identifier> source,
        final Optional<Identifier> organization,
        final Identifier project,
        final Optional<SemanticVersionRequirement> versionRequirement) {
        return new PartialDependency(
            source,
            organization,
            project,
            versionRequirement);
    }

    public static PartialDependency of(
        final Identifier source,
        final Identifier organization,
        final Identifier project,
        final SemanticVersionRequirement versionRequirement) {
        return new PartialDependency(
            Optional.of(source),
            Optional.of(organization),
            project,
            Optional.of(versionRequirement));
    }

    public static PartialDependency of(
        final Identifier organization,
        final Identifier project,
        final SemanticVersionRequirement versionRequirement) {
        return new PartialDependency(
            Optional.empty(),
            Optional.of(organization),
            project,
            Optional.of(versionRequirement));
    }

    public static PartialDependency of(
        final RecipeIdentifier recipeIdentifier) {
        return new PartialDependency(
            recipeIdentifier.source,
            Optional.of(recipeIdentifier.organization),
            recipeIdentifier.recipe,
            Optional.empty());
    }

    public static PartialDependency of(final Identifier organization, final Identifier project) {
        return new PartialDependency(
            Optional.empty(),
            Optional.of(organization),
            project,
            Optional.empty());
    }

    public static PartialDependency of(final Identifier project) {
        return new PartialDependency(
            Optional.empty(),
            Optional.empty(),
            project,
            Optional.empty());
    }
}
