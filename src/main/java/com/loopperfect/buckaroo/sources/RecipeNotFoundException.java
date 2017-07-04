package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.RecipeSource;

/**
 * Created by gaetano on 04/07/17.
 */
public class RecipeNotFoundException extends Exception {
    public RecipeSource source;
    public RecipeIdentifier identifier;

    public RecipeNotFoundException(final RecipeSource source, final RecipeIdentifier identifier) {
        super();
        this.source = Preconditions.checkNotNull(source);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public String getMessage() {
        return "RecipeNotFoundException: " + identifier.toString();
    }
}
