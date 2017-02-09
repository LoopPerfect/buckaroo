package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InstallRecipe implements Routine {

    public final Identifier projectToInstall;
    public final Optional<SemanticVersionRequirement> versionRequirement;

    public InstallRecipe(final Identifier projectToInstall, final Optional<SemanticVersionRequirement> versionRequirement) {
        this.projectToInstall = Preconditions.checkNotNull(projectToInstall);
        this.versionRequirement = Preconditions.checkNotNull(versionRequirement);
    }

    @Override
    public void execute() throws BuckarooException {

//        1. load the current buckaroo.json file into memory.
//           If it does not exist throw an error, maybe suggest install command
//        2. does the dependency already exist? If so, throw an exception, maybe suggest uninstall command
//        3. load the recipes from the recipes store
//        4. search for a version that matches the requirement (any if no version requirement is specified!)
//        5. if no version is found, report an error to the user; maybe suggest an upgrade
//        6. if a version is found, add it to the buckaroo.json
//        7. clone the repo into the buckaroo folder
//        8. report success!

        final Path projectFilePath = Paths.get(
                System.getProperty("user.dir"), "buckaroo.json");

        if (Files.notExists(projectFilePath) || !Files.isReadable(projectFilePath)) {
            throw new BuckarooException("We couldn't find the buckaroo.json file. Do you need to run buckaroo init? ");
        }

        if (!Files.isWritable(projectFilePath)) {
            throw new BuckarooException("We cannot write to the buckaroo.json file. Perhaps it has the wrong owner? ");
        }

        try {
            final Project project = Serializers.gson().fromJson(
                    com.google.common.io.Files.asCharSource(
                        projectFilePath.toFile(), Charset.defaultCharset()).read(),
                    Project.class);

            if (project.dependencies.containsKey(projectToInstall)) {
                throw new BuckarooException(
                        projectToInstall.name +
                                " is already installed. Remove it with buckaroo uninstall " +
                                projectToInstall.name);
            }

            final Path recipesPath = Paths.get(System.getProperty("user.home"), ".buckaroo/recipes");

            final ImmutableSet<Recipe> recipes = Recipes.find(recipesPath)
                    .orElseThrow(() -> new BuckarooException("Could not load any recipes. Please check " + recipesPath.normalize().toString()));

            System.out.println("Found " + recipes.size() + " recipe(s). Searching for the right version... ");

            final SemanticVersionRequirement versionRequirementForSearch =
                    versionRequirement.orElse(AnySemanticVersion.of());

            final Recipe recipe = recipes.stream()
                    .filter(x -> Objects.equals(projectToInstall, x.name))
                    .findFirst()
                    .orElseThrow(() -> new BuckarooException("Could not find a recipe for " + projectToInstall.name + ". Perhaps you should run buckaroo upgrade? "));

            final Map.Entry<SemanticVersion, RecipeVersion> recipeVersion = recipe.versions.entrySet().stream()
                    .filter(x -> versionRequirementForSearch.isSatisfiedBy(x.getKey()))
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .findFirst()
                    .orElseThrow(() -> new BuckarooException("Could not find a version that satisfies " + versionRequirementForSearch.encode() + ". Perhaps you should run buckaroo upgrade? "));

            final SemanticVersionRequirement versionRequirementToSave =
                    versionRequirement.orElse(ExactSemanticVersion.of(recipeVersion.getKey()));

            System.out.println("Found " + projectToInstall.name + " " + recipeVersion.getKey());

            System.out.println("Amending buckaroo.json... ");

            final Project amendedProject = project.addDependency(Dependency.of(projectToInstall, versionRequirementToSave));

            Files.write(projectFilePath, Serializers.gson(true).toJson(amendedProject).getBytes());

            System.out.println("Pulling the package... ");


        } catch (final IOException | JsonParseException e) {
            throw new BuckarooException(e);
        }

        System.out.println("Work in progress... ");
    }
}
