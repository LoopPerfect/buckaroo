package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.RecipeSource;
import com.loopperfect.buckaroo.RenderableException;
import com.loopperfect.buckaroo.views.GenericEventRenderer;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.ListLayout;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class RecipeFetchException extends Exception implements RenderableException {

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

    @Override
    public Component render() {

        final ImmutableList<Component> candidates =
            Streams.stream(source.findCandidates(identifier))
                .limit(3)
                .map(GenericEventRenderer::render)
                .collect(toImmutableList());

        if (candidates.size() > 0) {
            return StackLayout.of(
                Text.of("Error! \n" + getCause().toString(), Color.RED),
                Text.of("Maybe you meant to install one of the following?"),
                ListLayout.of(candidates));
        }

        return Text.of("Error! \n" + this.toString(), Color.RED);
    }
}
