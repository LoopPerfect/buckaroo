package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.RecipeIdentifier;

public final class FetchGithubProgressEvent extends Event {

    public final RecipeIdentifier identifier;
    public final Event progress;

    private FetchGithubProgressEvent(final RecipeIdentifier identifier, final Event event) {
        this.identifier = identifier;
        this.progress = event;
    }

    public static FetchGithubProgressEvent of(final RecipeIdentifier identifier, final Event event) {
        return new FetchGithubProgressEvent(identifier, event);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("identifier", identifier)
            .add("progress", progress)
            .toString();
    }

}
