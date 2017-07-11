package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.RecipeSource;

public class RecipeNotFoundException extends Exception {

    public final RecipeSource source;
    public final RecipeIdentifier identifier;

    public RecipeNotFoundException(final RecipeSource source, final RecipeIdentifier identifier) {

        super();

        this.source = Preconditions.checkNotNull(source);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    public RecipeNotFoundException(final RecipeSource source, final RecipeIdentifier identifier, final Throwable cause) {

        super(cause);

        this.source = Preconditions.checkNotNull(source);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public String getMessage() {
        return "RecipeNotFoundException: " + identifier.toString();
    }

    public static RecipeNotFoundException wrap(final RecipeSource source, final RecipeIdentifier identifier, final Throwable throwable) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(throwable);
        if (throwable instanceof RecipeNotFoundException) {
            final RecipeNotFoundException recipeNotFoundException = (RecipeNotFoundException) throwable;
            if (recipeNotFoundException.source.equals(source) && recipeNotFoundException.identifier.equals(identifier)) {
                return recipeNotFoundException;
            }
        }
        return new RecipeNotFoundException(source, identifier, throwable);
    }
}
