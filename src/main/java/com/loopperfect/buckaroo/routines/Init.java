package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.util.Optional;

import static com.loopperfect.buckaroo.Either.join;
import static com.loopperfect.buckaroo.Either.left;
import static com.loopperfect.buckaroo.Either.right;
import static com.loopperfect.buckaroo.Optionals.join;

public final class Init {

    private Init() {

    }

    private static final IO<Optional<Identifier>> readIdentifier = context -> {
        Preconditions.checkNotNull(context);
        while (true) {
            final Optional<String> x = IO.read().run(context);
            if (x.isPresent()) {
                final String candidate = x.get();
                if (Identifier.isValid(x.get())) {
                    return Optional.of(Identifier.of(x.get()));
                }
                if (candidate.length() < 3) {
                    IO.println("An identifier must have at least three characters. ")
                            .run(context);
                } else {
                    IO.println("An identifier may only contain letters, numbers, underscores and dashes. ")
                            .run(context);
                }
            } else {
                return Optional.empty();
            }
        }
    };

    private static IO<Optional<IOException>> createProjectFile(final String projectDirectory, final Identifier projectName) {
        Preconditions.checkNotNull(projectDirectory);
        Preconditions.checkNotNull(projectName);
        return Routines.writeProject(projectDirectory + "/buckaroo.json", Project.of(projectName), false);
    }

    public static IO<Either<IOException, Identifier>> askForProjectNameAndCreateProjectFile(final String projectDirectory) {
        Preconditions.checkNotNull(projectDirectory);
        return IO.println("What is the name of your project? ")
                        .then(readIdentifier)
                        .flatMap(x -> join(
                                x,
                                identifier -> IO.println("Creating buckaroo.json... ")
                                        .then(createProjectFile(projectDirectory, identifier)
                                                .flatMap(y -> join(
                                                        y,
                                                        error -> IO.value(left(error)),
                                                        () -> IO.println("Done. ")
                                                                .then(IO.value(right(identifier)))))),
                                () -> IO.value(left(new IOException("Could not get a project name. ")))));
    }

    public static final IO<Unit> routine = IO.of(x -> x.fs().workingDirectory())
            .flatMap(Init::askForProjectNameAndCreateProjectFile)
            .flatMap(x -> join(x, IO::println, r -> IO.noop()));
}
