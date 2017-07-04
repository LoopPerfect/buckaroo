package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import com.loopperfect.buckaroo.tasks.CommonTasks;
import io.reactivex.Single;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;

public final class LazyCookbookRecipeSource implements RecipeSource {

    private final Path path;

    private LazyCookbookRecipeSource(final Path path) {
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    @Override
    public Process<Event, Recipe> fetch(final RecipeIdentifier identifier) {

        Preconditions.checkNotNull(identifier);

        return Process.of( Single.fromCallable(() -> {
            if (identifier.source.isPresent()) {
                throw new IllegalArgumentException(identifier.encode() + " should be found on " + identifier.source.get());
            }
            return path.getFileSystem().getPath(
                path.toString(),
                "recipes",
                identifier.organization.name,
                identifier.recipe.name + ".json");
        }).flatMap( (pathToRecipe) -> {
            return CommonTasks.readRecipeFile(pathToRecipe)
                .map(Optional::of)
                .onErrorReturnItem(Optional.empty())
                .map(x-> {
                    if(x.isPresent()) return x.get();
                    throw new RecipeNotFoundException(this, identifier);
                });
        }));
    }

    @Override
    public Iterable<RecipeIdentifier> findCandidates(final RecipeIdentifier identifier) {
        final FileSystem fs = path.getFileSystem();
        try {
            final ImmutableList<RecipeIdentifier> candidates = CommonTasks.readCookBook(path);
            return Levenstein.findClosest(candidates, identifier);
        } catch (Throwable e) {
            return ImmutableList.of();
        }
    }


    public static RecipeSource of(final Path pathToCookbook) {
        return new LazyCookbookRecipeSource(pathToCookbook);
    }
}
