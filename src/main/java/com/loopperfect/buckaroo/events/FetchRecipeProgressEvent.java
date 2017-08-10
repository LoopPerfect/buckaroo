package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.RecipeIdentifier;

public final class FetchRecipeProgressEvent extends Event {

    public final RecipeIdentifier identifier;
    public final Event progress;

    private FetchRecipeProgressEvent(final RecipeIdentifier identifier, final Event event) {
        this.identifier = identifier;
        this.progress = event;
    }

    public static FetchRecipeProgressEvent of(final RecipeIdentifier identifier, final Event event) {
        return new FetchRecipeProgressEvent(identifier, event);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("identifier", identifier)
            .add("progress", progress)
            .toString();
    }

}
