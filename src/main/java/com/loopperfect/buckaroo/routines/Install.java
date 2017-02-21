package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

public final class Install {

    private Install() {

    }

    private static IO<Optional<IOException>> installRecipeVersion(
            final RecipeIdentifier identifier,
            final RecipeVersion recipeVersion,
            final ImmutableMap<Identifier, SemanticVersion> versions) {

        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(recipeVersion);
        Preconditions.checkNotNull(versions);

        return context -> {

            Preconditions.checkNotNull(context);

            // Checkout the source-code
            final Path localPath = Paths.get(
                    context.getWorkingDirectory().toString(),
                    "buckaroo/",
                    identifier.project.name,
                    "/",
                    identifier.version.toString(),
                    "/");

            final Optional<IOException> checkoutResult =
                    Routines.checkout(localPath, recipeVersion.gitCommit).run(context);

            if (checkoutResult.isPresent()) {
                return checkoutResult;
            }

            final ImmutableMap<Identifier, SemanticVersion> requiredVersions =
                    versions.entrySet().stream()
                            .filter(x -> recipeVersion.dependencies.requires(x.getKey()))
                            .collect(ImmutableMap.toImmutableMap(x -> x.getKey(), x -> x.getValue()));

            // Write the BUCKAROO_DEPS file
            final Path buckarooDepsPath = Paths.get(
                    context.getWorkingDirectory().toString(),
                    "buckaroo/",
                    identifier.project.name,
                    "/",
                    identifier.version.toString(),
                    "/",
                    "BUCKAROO_DEPS");

            final Optional<IOException> writeBuckarooDepsResult = context.writeFile(
                    buckarooDepsPath, Routines.buckarooDeps(requiredVersions), true);

            if (writeBuckarooDepsResult.isPresent()) {
                return writeBuckarooDepsResult;
            }

            // Write the .buckconfig.local file
            final Path buckconfigPath = Paths.get(
                    context.getWorkingDirectory().toString(),
                    "buckaroo/",
                    identifier.project.name,
                    "/",
                    identifier.version.toString(),
                    "/",
                    ".buckconfig.local");

            final Optional<IOException> writeConfigFileResult = context.writeFile(
                    buckconfigPath, Routines.buckConfig(requiredVersions), true);

            if (writeConfigFileResult.isPresent()) {
                return writeConfigFileResult;
            }

            // Success!
            return Optional.empty();
        };
    }

    public static IO<Optional<IOException>> installDependencies(final RecipeVersionFetcher fetcher, final ImmutableMap<Identifier, SemanticVersion> versions) {

        Preconditions.checkNotNull(fetcher);
        Preconditions.checkNotNull(versions);

        return context -> {

            Preconditions.checkNotNull(context);

            for (final Map.Entry<Identifier, SemanticVersion> entry : versions.entrySet()) {

                final RecipeIdentifier identifier = RecipeIdentifier.of(entry.getKey(), entry.getValue());

                final Optional<RecipeVersion> recipeVersionFetch =
                        fetcher.fetch(identifier);

                if (!recipeVersionFetch.isPresent()) {
                    return Optional.of(new IOException("Could not fetch recipe version for " + identifier));
                }

                final Optional<IOException> installationResult =
                        installRecipeVersion(identifier, recipeVersionFetch.get(), versions).run(context);

                if (installationResult.isPresent()) {
                    return installationResult;
                }
            }

            return Optional.empty();
        };
    }
}
