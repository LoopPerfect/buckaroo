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

import static com.loopperfect.buckaroo.Either.join;
import static com.loopperfect.buckaroo.Optionals.join;

public final class Quickstart {

    private Quickstart() {

    }

    private static Either<IOException, String> helloWorldCpp() {
        final URL url = Resources.getResource("com.loopperfect.buckaroo/HelloWorld.cpp");
        try {
            final String x = Resources.toString(url, Charsets.UTF_8);
            return Either.right(x);
        } catch (final IOException e) {
            return Either.left(e);
        }
    }

    private static IO<Optional<IOException>> createAppSkeleton(final String path, final Identifier projectName) {
        Preconditions.checkNotNull(projectName);
        return Routines.continueUntilPresent(ImmutableList.of(
            IO.createDirectory(path + "/"),
            IO.of(context -> context.fs().touch(path + "/" + ".buckconfig")),
            IO.of(context -> context.fs().touch(path + "/" + "BUCKAROO_DEPS")),
            IO.createDirectory(path + "/" + projectName + "/"),
            IO.createDirectory(path + "/" + projectName + "/src/"),
            IO.createDirectory(path + "/" + projectName + "/include/"),
            helloWorldCpp().join(
                error -> IO.value(Optional.of(error)),
                content -> IO.writeFile(path + "/" + projectName + "/src/main.cpp", content, false)),
            BuckFile.generate(projectName).join(
                error -> IO.value(Optional.of(error)),
                buck -> IO.writeFile(path + "/BUCK", buck, false))));
    }

    public static final IO<Unit> routine = IO.of(x -> x.fs().workingDirectory())
            .flatMap(path -> Init.askForProjectNameAndCreateProjectFile(path)
                    .flatMap(x -> join(
                            x,
                            IO::println,
                            identifier -> createAppSkeleton(path, identifier).flatMap(y -> join(
                                    y,
                                    IO::println,
                                    () -> InstallExisting.routine.next(IO.println("Done. Run your project with: ")
                                            .next(IO.println("buck apply :" + identifier.name))))))));
}
