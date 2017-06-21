package com.loopperfect.buckaroo;

import java.util.Objects;
import java.util.Optional;

public final class ResolvedDependencyReference {

    public final RecipeIdentifier identifier;
    public final Optional<String> target;

    private ResolvedDependencyReference(final RecipeIdentifier identifier, final Optional<String> target) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(target);
        this.identifier = identifier;
        this.target = target;
    }

    public String encode() {
        return identifier.source.map(x -> x.name + ".").orElse("") +
            identifier.organization.name + "." +
            identifier.recipe.name + "//:" +
            target.orElse(identifier.recipe.name);
    }

    public boolean equals(final ResolvedDependencyReference other) {
        Objects.requireNonNull(other);
        return Objects.equals(identifier, other.identifier) &&
            Objects.equals(target, other.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, target);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null &&
                obj instanceof ResolvedDependencyReference &&
                equals((ResolvedDependencyReference) obj);
    }

    @Override
    public String toString() {
        return encode();
    }

    public static ResolvedDependencyReference of(final RecipeIdentifier identifier, final Optional<String> target) {
        return new ResolvedDependencyReference(identifier, target);
    }

    public static ResolvedDependencyReference of(final RecipeIdentifier identifier, final String target) {
        return new ResolvedDependencyReference(identifier, Optional.of(target));
    }

    public static ResolvedDependencyReference of(final RecipeIdentifier identifier) {
        return new ResolvedDependencyReference(identifier, Optional.empty());
    }
}
