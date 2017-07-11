package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.RecipeSource;

public class RecipeFetchException extends Exception {

    public final RecipeSource source;
    public final RecipeIdentifier identifier;

    public RecipeFetchException(final RecipeSource source, final RecipeIdentifier identifier) {

        super();

        this.source = Preconditions.checkNotNull(source);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    public RecipeFetchException(final RecipeSource source, final RecipeIdentifier identifier, final Throwable cause) {

        super(cause);

        this.source = Preconditions.checkNotNull(source);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public String getMessage() {
        return "RecipeFetchException: " + identifier.toString();
    }

    public static RecipeFetchException wrap(final RecipeSource source, final RecipeIdentifier identifier, final Throwable throwable) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(throwable);
        if (throwable instanceof RecipeFetchException) {
            final RecipeFetchException recipeFetchException = (RecipeFetchException) throwable;
            if (recipeFetchException.source.equals(source) && recipeFetchException.identifier.equals(identifier)) {
                return recipeFetchException;
            }
        }
        return new RecipeFetchException(source, identifier, throwable);
    }
}
