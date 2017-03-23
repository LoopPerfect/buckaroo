package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;

import java.util.stream.Collectors;

import static com.loopperfect.buckaroo.routines.Routines.projectFilePath;

public final class Uninstall {

    private static IO<Unit> writeProject(final String path, final Project project) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(project);
        return Routines.writeProject(path, project, true)
            .flatMap(x -> Optionals.join(x,
                error -> IO.println("Error writing project file: ")
                    .then(IO.println(error)),
                IO::noop));
    }

    public static IO<Unit> routine(final Either<Identifier, RecipeIdentifier> identifier) {
        Preconditions.checkNotNull(identifier);
        return projectFilePath
            .flatMap(path ->
                Routines.readConfig(path).flatMap(
                    c -> c.join(
                        IO::println,
                        config -> Routines.readProject(path).flatMap(
                            x -> x.join(
                                error -> IO.println("Error loading project file: ").then(IO.println(error)),
                                project -> identifier.join(i -> {
                                    final ImmutableList<Dependency> candidates = project.dependencies.entries()
                                        .stream()
                                        .filter(d -> d.project.recipe.equals(i))
                                        .collect(ImmutableList.toImmutableList());
                                    if (candidates.isEmpty()) {
                                        return IO.println("No dependency on " + i.name + " was found! ");
                                    }
                                    if (candidates.size() > 1) {
                                        return IO.println("Multiple dependencies on " + i.name + " were found: ")
                                            .then(IO.println(candidates.stream()
                                                .map(Dependency::encode)
                                                .collect(Collectors.joining("\n"))));
                                    }
                                    return writeProject(path, project.removeDependency(candidates.get(0).project));
                                }, i -> {
                                    if (project.dependencies.requires(i)) {
                                        return writeProject(path, project.removeDependency(i));
                                    }
                                    return IO.println("No dependency on " + i.encode() + " was found. ");
                                }))))));
    }
}
