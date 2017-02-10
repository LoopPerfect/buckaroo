package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.loopperfect.buckaroo.Recipe;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.serialization.Serializers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public final class Routines {

    private Routines() {

    }

    private static Path recipesPath() {
        return Paths.get(System.getProperty("user.home"), ".buckaroo/recipes");
    }

    private static String formatRecipe(final Recipe recipe) {
        Preconditions.checkNotNull(recipe);
        return recipe.name.name +
                " " +
                recipe.versions.keySet().stream()
                        .map(u -> u.toString()).collect(Collectors.joining(", "));
    }

    public static final IO<Unit> listRecipes = IO.value(recipesPath())
            .flatMap(x -> IO.println("Searching for recipes in " + x + "... ").then(IO.value(x)))
            .flatMap(x -> IO.listFiles(x))
            .flatMap(x -> context -> x.flatMap(
                    y -> y.toString(),
                    y -> y.stream()
                            .filter(z -> Files.getFileExtension(z.toString()).equalsIgnoreCase("json"))
                            .distinct()
                            .map(z -> context.readFile(z)
                                    .rightProjection(w -> Serializers.gson().fromJson(w, Recipe.class))
                                    .join(w -> "Could not load " +  z.getFileName(), w -> formatRecipe(w)))
                            .collect(Collectors.joining("\n"))))
            .flatMap(x -> x.join(y -> IO.println(y), y -> IO.println(y)));
}
