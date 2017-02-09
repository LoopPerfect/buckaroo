package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.BuckarooException;
import com.loopperfect.buckaroo.Recipe;
import com.loopperfect.buckaroo.Recipes;
import com.loopperfect.buckaroo.Routine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ListRecipes implements Routine {

    public final Path recipeFolderPath;

    public ListRecipes(final Path recipeFolderPath) {
        Preconditions.checkNotNull(recipeFolderPath);
        this.recipeFolderPath = recipeFolderPath;
    }

    public ListRecipes() {
        this(Paths.get(System.getProperty("user.home"), ".buckaroo/recipes"));
    }

    @Override
    public void execute() throws BuckarooException {

        final Optional<ImmutableSet<Recipe>> recipes = Recipes.find(recipeFolderPath);

        if (!recipes.isPresent()) {
            throw new BuckarooException("The search for recipes failed \uD83D\uDE1E");
        }

        System.out.println("Found " + recipes.get().size() + " recipe(s)... ");

        for (final Recipe recipe : recipes.get()) {
            System.out.println();
            System.out.println(recipe.name);
            System.out.println(recipe.url);
            System.out.println(recipe.versions.keySet().stream()
                    .map(x -> x.toString()).collect(Collectors.joining(", ")));
        }
    }
}
