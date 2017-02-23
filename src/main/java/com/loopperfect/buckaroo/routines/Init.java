package com.loopperfect.buckaroo.routines;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.buck.BuckFile;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public final class Init {

    private Init() {

    }

    private static final IO<Optional<Identifier>> readIdentifier = context -> {
        Preconditions.checkNotNull(context);
        Optional<Identifier> result = Optional.empty();
        while (!result.isPresent()) {
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
            }
        }
        return Optional.empty();
    };

    private static Either<IOException, String> helloWorldCpp() {
        final URL url = Resources.getResource("com.loopperfect.buckaroo/HelloWorld.cpp");
        try {
            final String x = Resources.toString(url, Charsets.UTF_8);
            return Either.right(x);
        } catch (final IOException e) {
            return Either.left(e);
        }
    }

    private static IO<Optional<IOException>> createProjectFile(final Identifier projectName) {
        Preconditions.checkNotNull(projectName);
        return Routines.projectFilePath
                .flatMap(path -> Routines.writeProject(path, Project.of(projectName), false));
    }

    private static IO<Optional<IOException>> createAppSkeleton(final Identifier projectName) {
        Preconditions.checkNotNull(projectName);
        return IO.of(x -> x.fs().workingDirectory())
                .flatMap(path -> Routines.continueUntilPresent(ImmutableList.of(
                        IO.createDirectory(path + "/"),
                        IO.createDirectory(path + "/" + projectName + "/"),
                        IO.createDirectory(path + "/" + projectName + "/src/"),
                        IO.createDirectory(path + "/" + projectName + "/include/"),
                        helloWorldCpp().join(
                                error -> IO.value(Optional.of(error)),
                                content -> IO.writeFile(path + "/" + projectName + "/src/main.cpp", content, false)),
                        BuckFile.generate(projectName).join(
                                error -> IO.value(Optional.of(error)),
                                buck -> IO.writeFile(path + "/BUCK", buck, false)))));
    }

    public static final IO<Unit> routine = Routines.projectFilePath
            .flatMap(path ->
                    IO.println("What is the name of your project? ")
                            .then(readIdentifier)
                            .flatMap(x -> Optionals.join(
                                    x,
                                    identifier -> Routines.continueUntilPresent(ImmutableList.of(
                                            createProjectFile(identifier),
                                            createAppSkeleton(identifier))).flatMap(y -> Optionals.join(
                                            y,
                                            IO::println,
                                            () -> IO.println("Done. Run your project with: ")
                                                    .then(IO.println("buck run :" + identifier.name)))),
                                    IO::noop)));
}
