package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import com.loopperfect.buckaroo.versioning.ExactSemanticVersion;

import java.util.Optional;

import static com.loopperfect.buckaroo.routines.Routines.configFilePath;
import static com.loopperfect.buckaroo.routines.Routines.projectFilePath;

@Deprecated
public final class Install {

    private Install() {

    }

    private static Either<ImmutableList<DependencyResolutionException>, ImmutableMap<RecipeIdentifier, SemanticVersion>> resolvedDependencies(
            final DependencyGroup dependencyGroup, final ImmutableList<Cookbook> cookBooks) {
        Preconditions.checkNotNull(dependencyGroup);
        Preconditions.checkNotNull(cookBooks);
//        final DependencyFetcher fetcher = CookbookDependencyFetcher.of(cookBooks);
//        return DependencyResolver.resolve(dependencyGroup, fetcher);
        return null;
    }

    public static IO<Unit> routine(final RecipeIdentifier identifier, final Optional<SemanticVersionRequirement> version) {
        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(version);
        final SemanticVersionRequirement versionRequirementToUse =
            version.orElseGet(AnySemanticVersion::of);
        final Dependency dependencyToTry = Dependency.of(
            identifier, versionRequirementToUse);

        final IO<Unit> installDependency = IO.println("Adding dependency on " + identifier.encode() +
            Optionals.join(version, x -> "@" + x.encode(), () -> "") + "... ")
            .next(projectFilePath)
            .flatMap(path -> Routines.readProject(path)
                .flatMap(x -> x.join(
                    error -> IO.println("Could not read buckaroo.json. Are you in the right folder? ")
                        .next(IO.println(error)),
                    project -> configFilePath.flatMap(Routines::readConfig)
                        .flatMap(y -> y.join(
                            error -> IO.println("Could not read the config. ")
                                .next(IO.println(error)),
                            config -> Routines.readCookBooks(config).flatMap(z -> z.join(
                                error -> IO.println("Could not read cookbooks. ")
                                    .next(IO.println(error)),
                                cookBooks -> resolvedDependencies(project.dependencies.addDependency(dependencyToTry), cookBooks).join(
                                    error -> IO.println("Could not resolve a dependency. ")
                                        .next(IO.println(error)),
                                    resolvedDependencies ->
                                        Routines.writeProject(
                                            path,
                                            project.addDependency(
                                                Dependency.of(
                                                    identifier,
                                                    version.orElseGet(() -> ExactSemanticVersion.of(
                                                        resolvedDependencies.get(identifier))))),
                                            true)
                                            .flatMap(w -> Optionals.join(
                                                w,
                                                error -> IO.println("Could not write buckaroo.json. ")
                                                    .next(IO.println(error)),
                                                () -> IO.println("Done. ")
                                                    .next(IO.println("Installing dependencies... ")
                                                    .next(InstallExisting.routine))))))))))));

        return Routines.ensureConfig.flatMap(
            e -> Optionals.join(
                e,
                i -> IO.println("Error installing default Buckaroo config... ").next(IO.println(i)),
                () -> installDependency));
    }
}
