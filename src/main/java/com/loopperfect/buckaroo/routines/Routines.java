package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.buck.BuckFile;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.io.IOContext;
import com.loopperfect.buckaroo.serialization.Serializers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Routines {

    private Routines() {

    }

    public static final IO<Either<IOException, BuckarooConfig>> loadConfig = context -> {
        Preconditions.checkNotNull(context);
        final Path configPath = Paths.get(context.getUserHomeDirectory().toString(), ".buckaroo/", "config.json");
        final Either<IOException, String> readFileResult = context.readFile(configPath);
        return readFileResult.join(
                e -> Either.left(e),
                file -> {
                    try {
                        final BuckarooConfig config = Serializers.gson().fromJson(file, BuckarooConfig.class);
                        return Either.right(config);
                    } catch (final JsonParseException e) {
                        return Either.left(new IOException(e));
                    }
                });
    };

    private static final Either<IOException, Recipe> readRecipe(final IOContext context, final Path path) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(context);
        return context.readFile(path).join(
                x -> Either.left(x),
                x -> {
                    try {
                        return Either.right(Serializers.gson().fromJson(x, Recipe.class));
                    } catch (final JsonParseException e) {
                        return Either.left(new IOException(e));
                    }
                });
    }

    // TODO: Refactor to make this more functional!
    private static IO<Either<IOException, ImmutableList<Either<IOException, Recipe>>>> loadRecipesForCookBook(final Identifier cookBook) {
        Preconditions.checkNotNull(cookBook);
        return context -> {
                Preconditions.checkNotNull(context);
                final Path recipesDirectory = Paths.get(context.getUserHomeDirectory().toString(), ".buckaroo/", cookBook.name,"/recipes");
                final Either<IOException, ImmutableList<Path>> listFiles = context.listFiles(recipesDirectory);
                return listFiles.map(
                        x -> x,
                        x -> ImmutableList.copyOf(x
                                .stream()
                                .filter(i -> isJsonFile(i))
                                .map(i -> i.normalize())
                                .distinct()
                                .map(i -> readRecipe(context, i))
                                .collect(Collectors.toList())));
        };
    }

    // TODO: Refactor to make this more functional
    private static IO<Either<IOException, ImmutableList<Either<IOException, Recipe>>>> loadRecipes = context -> {
        Preconditions.checkNotNull(context);
        final Either<IOException, BuckarooConfig> loadConfigResult = loadConfig.run(context);
        return loadConfigResult.rightProjection(config -> {
            final List<Either<IOException, Recipe>> r = new ArrayList<>();
            for (final RemoteCookBook cookBook : config.cookBooks) {
                final Either<IOException, ImmutableList<Either<IOException, Recipe>>> loadRecipesResult =
                        loadRecipesForCookBook(cookBook.name).run(context);
                r.addAll(loadRecipesResult.join(
                        error -> ImmutableList.of(),
                        recipes -> recipes));
            }
            return ImmutableList.copyOf(r);
        });
    };

    private static IO<Path> recipesPath(final Identifier cookBook) {
        Preconditions.checkNotNull(cookBook);
        return context -> {
            Preconditions.checkNotNull(context);
            return Paths.get(context.getUserHomeDirectory().toString(), ".buckaroo/", cookBook.name, "/recipes/");
        };
    }

    private static String formatRecipe(final Recipe recipe) {
        Preconditions.checkNotNull(recipe);
        return recipe.name.name +
                " " +
                recipe.versions.keySet().stream()
                        .map(u -> u.toString()).collect(Collectors.joining(", "));
    }

    private static boolean isJsonFile(final Path path) {
        Preconditions.checkNotNull(path);
        return Files.getFileExtension(path.toString()).equalsIgnoreCase("json");
    }

    public static final IO<Unit> listRecipesForCookBook(final Identifier cookBook) {
        Preconditions.checkNotNull(cookBook);
        return recipesPath(cookBook)
                .flatMap(x -> IO.println("Searching for recipes in " + x + "... ").then(IO.value(x)))
                .flatMap(x -> IO.listFiles(x))
                .flatMap(x -> context -> x.map(
                        y -> y.toString(),
                        y -> y.stream()
                                .filter(z -> Files.getFileExtension(z.toString()).equalsIgnoreCase("json"))
                                .distinct()
                                .map(z -> context.readFile(z)
                                        .rightProjection(w -> Serializers.gson().fromJson(w, Recipe.class))
                                        .join(w -> "Could not load " + z.getFileName(), w -> formatRecipe(w)))
                                .collect(Collectors.joining("\n"))))
                .flatMap(x -> x.join(y -> IO.println(y), y -> IO.println(y)));
    }

    public static final IO<Unit> listRecipes = context -> {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context);
        final Path configPath = Paths.get(context.getUserHomeDirectory().toString(), ".buckaroo/", "config.json");
        final Either<IOException, String> readFileResult = context.readFile(configPath);
        return readFileResult.join(
                error -> {
                    context.println("Error reading config.json");
                    context.println(error.toString());
                    return Unit.of();
                },
                file -> {
                    try {
                        final BuckarooConfig config = Serializers.gson().fromJson(file, BuckarooConfig.class);
                        for (final RemoteCookBook cookBook : config.cookBooks) {
                            listRecipesForCookBook(cookBook.name).run(context);
                        }
                    } catch (final JsonParseException e) {
                        context.println("Error parsing config.json");
                        context.println(e.toString());
                    }
                    return Unit.of();
                });
    };

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
            .flatMap(x -> IO.println("What is the name of your project? "))
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

    private static final IO<Either<IOException, Project>> readProjectFile =
            IO.readFile(Paths.get("buckaroo.json"))
                    .map(x -> x.join(
                            y -> Either.left(y),
                            y -> {
                                try {
                                    return Either.right(Serializers.gson().fromJson(y, Project.class));
                                } catch (final JsonParseException e) {
                                    return Either.left(new IOException(e));
                                }
                            }));

    private static final IO<Optional<IOException>> writeProjectFile(final Project project) {
            return context -> context.writeFile(
                    Paths.get("buckaroo.json"),
                    Serializers.gson(true).toJson(project),
                    true);
    }

    // TODO: Do we want to modify the BUCK file?
    // We need to do a recursive dependency search!
    public static final IO<Unit> installDependency(final Identifier projectToInstall, final Optional<SemanticVersionRequirement> versionRequirement) {
        Preconditions.checkNotNull(projectToInstall);
        Preconditions.checkNotNull(versionRequirement);
        // First read the buckaroo.json file
        return readProjectFile
                .flatMap(x -> x.join(
                        // Print out any error
                        error -> IO.println(error.toString()),
                        // Read the project file...
                        project -> {
                            // Is it already installed?
                            if (project.dependencies.containsKey(projectToInstall)) {
                                // Yes
                                return IO.println("That library is already a dependency! Version " +
                                        project.dependencies.get(projectToInstall).encode());
                            }
                            // No, so load the recipes...
                            return loadRecipes.flatMap(y -> y.join(
                                    // Loading the recipes failed; print the error
                                    error -> IO.println(error),
                                    // Success!
                                    recipes -> {
                                        final ImmutableSet<SemanticVersion> versions =
                                                recipes.stream()
                                                        .filter(i -> i.join(
                                                                j -> false,
                                                                j -> j.name.equals(projectToInstall)
                                                        ))
                                                        .findAny()
                                                        .flatMap(i -> i.rightProjection(j -> j.versions.keySet()).toOptional())
                                                        .orElseGet(() -> ImmutableSet.of());
                                        final SemanticVersionRequirement versionToFind =
                                                versionRequirement.orElse(AnySemanticVersion.of());
                                        final Optional<SemanticVersion> versionToUse =
                                                SemanticVersions.resolve(versions, ImmutableSet.of(versionToFind));
                                        // Did we find a version to use?
                                        if (!versionToUse.isPresent()) {
                                            // Nope
                                            return IO.println("Not version of " + projectToInstall +
                                                    " that satisfies " + versionToFind.encode() + " could be found. " +
                                                    "Perhaps you should run buckaroo upgrade? ");
                                        }
                                        // Yes!
                                        final Dependency dependency = Dependency.of(
                                                projectToInstall,
                                                versionRequirement.orElseGet(
                                                        () -> ExactSemanticVersion.of(versionToUse.get())));
                                        final Project modifiedProject = project.addDependency(dependency);
                                        // Write the project file; print done or error
                                        return IO.println("Installing " + projectToInstall.name + "@" + versionToUse.get() + "... ")
                                                .then(writeProjectFile(modifiedProject))
                                                .flatMap(i -> IO.println(i.map(j -> j.toString()).orElse("Done!")));
                                    }
                            ));
                        }));
    }

    private static final IO<Optional<Exception>> checkout(final Path localPath, final String branch) {
        Preconditions.checkNotNull(localPath);
        Preconditions.checkNotNull(branch);
        return context -> context.git().checkout(localPath.toFile(), branch);
    }

    private static final IO<Optional<Exception>> clone(final Path localPath, final String gitUrl) {
        Preconditions.checkNotNull(localPath);
        Preconditions.checkNotNull(gitUrl);
        return context -> context.git().clone(localPath.toFile(), gitUrl);
    }

    public static final IO<Unit> upgrade(final RemoteCookBook cookBook) {
        Preconditions.checkNotNull(cookBook);
        return context -> {
            Preconditions.checkNotNull(context);
            final Path recipesFolder = Paths.get(
                    context.getUserHomeDirectory().toString(),
                    ".buckaroo/",
                    cookBook.name.name);
            // Tell the user what we are up to...
            context.println("Upgrading the Buckaroo recipes registry... ");
            // Try to checkout master...
            context.println("Switching to master... ");
            final Optional<Exception> checkoutResult = context.git()
                    .checkout(recipesFolder.toFile(), "master");
            // If we fail, try to clone
            if (checkoutResult.isPresent()) {
                context.println("Failed! ");
                context.println("Cloning " + cookBook.url + "... ");
                final Optional<Exception> cloneResult = context.git().clone(recipesFolder.toFile(), cookBook.url);
                // If we fail, print an error and stop
                if (cloneResult.isPresent()) {
                    context.println("Could not prepare the recipes folder. Perhaps you should delete it? ");
                    context.println(cloneResult.get().toString());
                    return Unit.of();
                }
            } else {
                // If we could checkout master, try to pull
                context.println("Pulling from " + cookBook.url + "... ");
                final Optional<Exception> pullResult = context.git().pull(recipesFolder.toFile());
                if (pullResult.isPresent()) {
                    context.println("We could not pull the latest recipes. ");
                    context.println(pullResult.get().toString());
                    return Unit.of();
                }
            }
            context.println("Success!");
            return Unit.of();
        };
    }

    public static final IO<Unit> uninstallRecipe(final Identifier recipe) {
        Preconditions.checkNotNull(recipe);
        return context -> {
            Preconditions.checkNotNull(context);
            context.println("Uninstalling " + recipe.name + "... ");
            final Either<IOException, Project> projectFile = readProjectFile.run(context);
            return projectFile.join(
                    error -> {
                        context.println("Could not load buckaroo.json. Are you in the right folder? ");
                        return Unit.of();
                    },
                    project -> {
                        if (project.dependencies.containsKey(recipe)) {
                            final SemanticVersionRequirement versionRequirement = project.dependencies.get(recipe);
                            context.println("Found a dependency on " + recipe.name + " " + versionRequirement.encode());
                            final Project modifiedProject = project.removeDependency(recipe);
                            context.println("Writing the modified project file... ");
                            writeProjectFile(modifiedProject).run(context);
                            context.println("Done.");
                        } else {
                            context.println("Could not find " + recipe.name + " in this project's dependencies. ");
                        }
                        return Unit.of();
                    });
        };
    }

    // Load the projects file
    //
    public static final IO<Unit> installExisting = context -> {
        Preconditions.checkNotNull(context);
        // Load the project file
        final Either<IOException, Project> projectFile = readProjectFile.run(context);
        // Did it succeed?
        return projectFile.join(
                // No; show an error message
                error -> {
                    context.println("Could not load buckaroo.json. Are you in the right folder? ");
                    return Unit.of();
                },
                project -> {
                    context.println("Loaded the buckaroo.json file. ");
                    // Load the recipes file
                    final Either<IOException, ImmutableList<Either<IOException, Recipe>>> recipesFile =
                            loadRecipes.run(context);
                    // Did we succeed?
                    return recipesFile.join(
                            // Nope
                            error -> {
                                // Show an error
                                context.println("Could not load the recipes file. ");
                                return Unit.of();
                            },
                            // Yes!
                            recipes -> {
                                // TODO: We need to get implicit dependencies!
                                // Ignore the recipes that failed to load
                                final ImmutableList<Recipe> resolvedRecipes = ImmutableList.copyOf(recipes.stream()
                                        .flatMap(x -> x.join(e -> Stream.empty(), r -> Stream.of(r)))
                                        .collect(Collectors.toList()));
                                // For each dependency in the project, we need to
                                // find the versions in the recipe book
                                // and take the highest compatible version.
                                for (final Map.Entry<Identifier, SemanticVersionRequirement> dependency : project.dependencies.entrySet()) {
                                    final Optional<RecipeVersion> recipeVersion = resolvedRecipes.stream()
                                            .filter(x -> dependency.getKey().equals(x.name))
                                            .flatMap(x -> x.versions.entrySet().stream())
                                            .filter(x -> dependency.getValue().isSatisfiedBy(x.getKey()))
                                            .sorted(Comparator.comparing(Map.Entry::getKey))
                                            .map(x -> x.getValue())
                                            .findFirst();
                                    if (!recipeVersion.isPresent()) {
                                        context.println("Could not find a version of " + dependency.getKey() +
                                                " that satisfies " + dependency.getValue().encode() + ". ");
                                        return Unit.of();
                                    }
                                    final GitCommit gitCommit = recipeVersion.get().gitCommit;
                                    // Let's pull it from git...
                                    context.println("Cloning " + gitCommit.url + "... ");
                                    final Path dependencyPath = Paths.get(
                                            context.getWorkingDirectory().toString(),
                                            "buckaroo/",
                                            dependency.getKey().name);
                                    final Optional<Exception> cloneResult = context.git().clone(
                                            dependencyPath.toFile(),
                                            gitCommit.url);
                                    if (cloneResult.isPresent()) {
                                        context.println("Failed to clone " + gitCommit.url + ". ");
                                        context.println(cloneResult.get().toString());
                                        return Unit.of();
                                    }
                                    context.println("Done. ");
                                    context.println("Checking out " + gitCommit.commit + "... ");
                                    final Optional<Exception> checkoutResult = context.git()
                                            .checkout(dependencyPath.toFile(), gitCommit.commit);
                                    if (checkoutResult.isPresent()) {
                                        context.println("Failed to checkout " + gitCommit.commit + ". ");
                                        context.println(checkoutResult.get().toString());
                                        return Unit.of();
                                    }
                                    context.println("Done. ");
                                }
                                return Unit.of();
                            });
                });
    };

    public static final IO<Unit> generateBuckFile = context -> {
        Preconditions.checkNotNull(context);
        context.println("Generating BUCK file... ");
        final Either<IOException, Project> projectFile = readProjectFile.run(context);
        return projectFile.join(
                error -> {
                    context.println("Could not load buckaroo.json. Are you in the right folder? ");
                    return Unit.of();
                },
                project -> {
                    final Either<IOException, String> buckFile = BuckFile.generate(project);
                    return buckFile.join(error -> {
                        context.println("Could not generate the BUCK file! ");
                        context.println(error.toString());
                        return Unit.of();
                    }, buckString -> {
                        final Path path = Paths.get(context.getWorkingDirectory().toString(), "BUCK");
                        final Optional<IOException> writeResult = context.writeFile(path, buckString);
                        if (writeResult.isPresent()) {
                            context.println("Could not write the BUCK file.");
                            context.println(writeResult.get().toString());
                            return Unit.of();
                        }
                        context.println("Done. ");
                        return Unit.of();
                    });
                });
    };

    public static final IO<Unit> listCookBooks =
            loadConfig.flatMap(x -> x.join(
                    error -> IO.println("Error loading config.json").then(IO.println(error)),
                    config -> context -> {
                        for (final RemoteCookBook cookBook : config.cookBooks) {
                            context.println(cookBook.name.name);
                            context.println(cookBook.url);
                            context.println();
                        }
                        return Unit.of();
                    }));
}
