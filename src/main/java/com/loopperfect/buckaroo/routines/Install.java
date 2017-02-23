package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;

import java.util.Optional;

import static com.loopperfect.buckaroo.routines.Routines.configFilePath;
import static com.loopperfect.buckaroo.routines.Routines.projectFilePath;

public final class Install {

    private Install() {

    }

    private static Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>> resolvedDependencies(
            final DependencyGroup dependencyGroup, final ImmutableList<CookBook> cookBooks) {
        Preconditions.checkNotNull(dependencyGroup);
        Preconditions.checkNotNull(cookBooks);
        final DependencyFetcher fetcher = CookbookDependencyFetcher.of(cookBooks);
        return DependencyResolver.resolve(dependencyGroup, fetcher);
    }

    public static IO<Unit> routine(final Identifier identifier, final Optional<SemanticVersionRequirement> version) {
        Preconditions.checkNotNull(identifier);
        Preconditions.checkNotNull(version);
        final SemanticVersionRequirement versionRequirementToUse =
                version.orElseGet(AnySemanticVersion::of);
        final Dependency dependencyToTry = Dependency.of(
                identifier, versionRequirementToUse);
        return IO.println("Adding dependency on " + identifier.name +
                Optionals.join(version, x -> "@" + x.encode(), () -> "") + "... ")
                .then(projectFilePath)
                .flatMap(path -> Routines.readProject(path)
                        .flatMap(x -> x.join(
                                error -> IO.println("Could not read buckaroo.json. Are you in the right folder? ")
                                        .then(IO.println(error)),
                                project -> configFilePath.flatMap(Routines::readConfig)
                                        .flatMap(y -> y.join(
                                                error -> IO.println("Could not read the config. ")
                                                        .then(IO.println(error)),
                                                config -> Routines.readCookBooks(config).flatMap(z -> z.join(
                                                        error -> IO.println("Could not read cookbooks. ")
                                                                .then(IO.println(error)),
                                                        cookBooks -> resolvedDependencies(project.dependencies.addDependency(dependencyToTry), cookBooks).join(
                                                                error -> IO.println("Could not resolve a dependency. ")
                                                                        .then(IO.println(error)),
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
                                                                                                .then(IO.println(error)),
                                                                                        () -> IO.println("Done. ")
                                                                                                .then(IO.println("Installing dependencies... ")
                                                                                                .then(InstallExisting.routine))))))))))));
    }
}
