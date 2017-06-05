package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.util.Optional;
import java.nio.file.Path;

import static com.loopperfect.buckaroo.Optionals.join;

@Deprecated
public final class Init {

    private Init() {

    }

    private static IO<Optional<IOException>> createProjectFile(final String projectDirectory, final Optional<String> projectName) {
        Preconditions.checkNotNull(projectDirectory);
        Preconditions.checkNotNull(projectName);
        return Routines.writeProject(projectDirectory + "/buckaroo.json", Project.of(projectName), false);
    }

    public static IO<Either<IOException, Optional<String>>> generateProjectNameAndCreateProjectFile(final String projectDirectory) {
        Preconditions.checkNotNull(projectDirectory);
        return IO.of(c -> c.console().println("Creating buckaroo.json... "))
            .next(c -> {
                final Path p = c.fs().getPath(projectDirectory);
                final Optional<String> projectName = p.getNameCount() > 0 ?
                    Optional.of(p.getName(p.getNameCount() - 1).toString()) :
                    Optional.empty();
                return projectName;
            })
            .flatMap(projectName -> createProjectFile(projectDirectory, projectName)
                .flatMap(e -> e.isPresent() ?
                    IO.value(Either.left(e.get())) :
                    IO.of(c -> c.fs().touch(projectDirectory + "/" + ".buckconfig"))
                        .map(a -> a.isPresent() ?
                            Either.left(a.get()) :
                            Either.right(projectName))));
    }

    public static final IO<Unit> routine = IO.of(x -> x.fs().workingDirectory())
            .flatMap(Init::generateProjectNameAndCreateProjectFile)
            .flatMap(x -> x.join(e -> IO.println(e), ignored -> IO.println("Done. ")));
}
