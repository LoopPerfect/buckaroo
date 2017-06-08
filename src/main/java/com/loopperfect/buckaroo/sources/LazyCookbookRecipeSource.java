package com.loopperfect.buckaroo.sources;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Recipe;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.RecipeSource;
import com.loopperfect.buckaroo.tasks.CommonTasks;
import io.reactivex.Single;

import java.nio.file.Path;

public final class LazyCookbookRecipeSource implements RecipeSource {

    private final Path path;

    private LazyCookbookRecipeSource(final Path path) {
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    @Override
    public Single<Recipe> fetch(final RecipeIdentifier identifier) {

        Preconditions.checkNotNull(identifier);

        return Single.fromCallable(() -> {
            if (identifier.source.isPresent()) {
                throw new IllegalArgumentException(identifier.encode() + " should be found on " + identifier.source.get());
            }
            return path.getFileSystem().getPath(
                path.toString(),
                "recipes",
                identifier.organization.name,
                identifier.recipe.name + ".json");
        }).flatMap(CommonTasks::readRecipeFile);
    }

    public static RecipeSource of(final Path pathToCookbook) {
        return new LazyCookbookRecipeSource(pathToCookbook);
    }
}
