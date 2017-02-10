package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.Project;
import com.loopperfect.buckaroo.Recipe;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.serialization.Serializers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
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

    public static final IO<Unit> listRecipes = IO.of(context -> Paths.get(context.getUserHomeDirectory().toString(), ".buckaroo/recipes"))
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

    private static final String invalidProjectNameWarning =
            "A project name may only contain letters, numbers, underscores and dashes. " +
                    "It must be between 3 and 30 characters. ";

    private static final IO<Identifier> requestIdentifier = IO.read()
            .map(x -> x.flatMap(y -> Identifier.parse(y)))
            .flatMap(x -> x.isPresent() ? IO.value(x) : IO.println(invalidProjectNameWarning).then(IO.value(x)))
            .until(x -> x.isPresent())
            .map(x -> x.get());

    // TODO: Get license
    public static final IO<Unit> createProjectSkeleton = IO.of(context -> context.getWorkingDirectory())
            .flatMap(x -> IO.println("Creating a project in " + x + "... "))
            .flatMap(x -> IO.println("What is the name of your project? ").then(IO.value(x)))
            .then(requestIdentifier)
            .flatMap(x -> IO.println(x).then(IO.value(x)))
            .map(x -> Project.of(x, Optional.empty(), ImmutableMap.of()))
            .flatMap(x -> IO.println("Creating the buckaroo.json file... ").then(IO.value(x)))
            .map(x -> Serializers.gson(true).toJson(x))
            .flatMap(x -> IO.writeFile(Paths.get("buckaroo.json"), x))
            .flatMap(x -> x.isPresent() ?
                    IO.println(x.get()) :
                    IO.println("Done! ")
                            .then(IO.println("Creating the modules directory... "))
                            .then(IO.createDirectory(Paths.get("./buckaroo")))
                            .flatMap(y -> y.isPresent() ?
                                    IO.println(y.get()) :
                                    IO.println("Done!")
                                            .then(IO.println("Make sure you add buckaroo.json and buckaroo/ to your .gitignore"))));
}
