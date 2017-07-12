package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.*;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Created by gaetano on 12/07/17.
 */
public class PartialDependencyResolutionException
    extends Exception
    implements RenderableException {
    final ImmutableList<RecipeIdentifier> candidates;
    final PartialDependency requested;

    private PartialDependencyResolutionException(
        final ImmutableList<RecipeIdentifier> candidates,
        final PartialDependency requested) {
        this.candidates = Preconditions.checkNotNull(candidates);
        this.requested = Preconditions.checkNotNull(requested);
    }

    public static PartialDependencyResolutionException of(
        final ImmutableList<RecipeIdentifier> candidates,
        final PartialDependency requested) {
        return new PartialDependencyResolutionException(
            candidates,
            requested
        );
    }

    @Override
    public Component render() {
        if (candidates.size() == 0) {
            return StackLayout.of(
                Text.of("PartialDependencyCompletionError!", Color.RED),
                FlowLayout.of(
                    Text.of("Could not find what you are looking for... you requested: "),
                    Text.of(requested.toString(), Color.YELLOW)),
                Text.of("This might help:"),
                ListLayout.of(
                    Text.of("Ensure you don't have a typo"),
                    FlowLayout.of(
                        Text.of("Specify an organization eg.: "), Text.of("boost/",Color.MAGENTA), Text.of("asio")),
                    FlowLayout.of(
                        Text.of("Specify an source eg.: "), Text.of("github+boost/",Color.MAGENTA), Text.of("asio")),
                    FlowLayout.of(
                        Text.of("Visit "),
                        Text.of("https://buckaroo.pm", Color.BLUE)
                    )
                )
            );
        }

        if (candidates.size() > 1) {
            return StackLayout.of(
                FlowLayout.of(
                    Text.of("PartialDependencyCompletionError!", Color.RED),
                    Text.of("Your request for "),
                    Text.of(requested.toString(), Color.BLUE),
                    Text.of(" is ambiguous")),
                Text.of("do you want to install one of these packages?"),
                ListLayout.of(
                    candidates.stream().map(
                        c-> FlowLayout.of(
                                Text.of(c.organization.toString(), Color.MAGENTA),
                                Text.of("/"),
                                Text.of(c.recipe.toString())
                        )).collect(toImmutableList())));
        }

        return StackLayout.of(
            Text.of("PartialDependencyCompletionError!", Color.RED),
            Text.of("Sry but something went wrong resolving: " + candidates.get(0).toString(), Color.RED),
            FlowLayout.of(
                Text.of("please report to "),
                Text.of("https://github.com/loopperfect/buckaroo/issues", Color.BLUE)
            )
        );
    }
}
