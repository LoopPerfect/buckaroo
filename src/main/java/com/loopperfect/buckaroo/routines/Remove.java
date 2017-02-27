package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;

import java.util.stream.Collectors;

import static com.loopperfect.buckaroo.routines.Routines.buckarooDirectory;
import static com.loopperfect.buckaroo.routines.Routines.projectFilePath;
import static com.loopperfect.buckaroo.routines.Routines.writeProject;

/**
 * Created by gaetano on 24/02/17.
 */
public final class Remove {

    public static final IO<Unit> Routine(final Identifier identifier) {
        Preconditions.checkNotNull(identifier);

        return projectFilePath
            .flatMap(path ->
                Routines.readConfig(path).flatMap(
                    c -> c.join(
                        IO::println,
                        config -> Routines.readProject(path).flatMap(
                        x -> x.join(
                            error -> IO.println("error loading project file: ").then(IO.println(error)),
                            project -> writeProject(path, project.removeDependency(identifier), true)
                                .map(y -> (y.isPresent()) ?
                                    Either.left(y.get()) :
                                    Either.right(1))
                                .flatMap(y -> y.join(
                                    error -> IO.println("error writing project file: ").then(IO.println(error)),
                                    $ -> Routines.readCookBooks(config).flatMap(z-> z.join(
                                        error-> IO.println("error loading cookbook").then(IO.println(error)),
                                        books -> {
                                            final ImmutableSet<Identifier> depsToRemove = DependencyResolver
                                                .removableDependencies(
                                                    project.dependencies.dependencies,
                                                    identifier,
                                                    CookbookDependencyFetcher.of(books));

                                            return buckarooDirectory.flatMap(
                                                dir-> IO.sequence(depsToRemove.stream().map(
                                                    dep->IO.deleteFile(dir+"/"+dep.name)
                                            ).collect(ImmutableList.toImmutableList())).ignore());
                                        })))))))));
    }
}
