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
}
