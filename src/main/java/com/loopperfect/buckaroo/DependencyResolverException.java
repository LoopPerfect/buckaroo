package com.loopperfect.buckaroo;

public class DependencyResolverException extends BuckarooException {

    protected final RecipeIdentifier id;

    protected DependencyResolverException(final RecipeIdentifier id, final String message) {
        super(message);
        this.id = id;
    }

    public final RecipeIdentifier getIdentifier() {
        return id;
    }

}
