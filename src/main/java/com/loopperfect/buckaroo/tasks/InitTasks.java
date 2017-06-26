package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.serialization.Serializers;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;

public final class InitTasks {

    private InitTasks() {

    }

    public static Single<ReadProjectFileEvent> generateProjectForDirectory(final Path path) {

        Preconditions.checkNotNull(path);

        return Single.fromCallable(() -> {
            final Optional<String> projectName = path.getNameCount() > 0 ?
                Optional.of(path.getName(path.getNameCount() - 1).toString()) :
                Optional.empty();
            return ReadProjectFileEvent.of(Project.of(projectName));
        });
    }

    public static Observable<Event> init(final Path projectDirectory) {

        Preconditions.checkNotNull(projectDirectory);

        return MoreObservables.chain(

            // Create an empty project from the working directory
            generateProjectForDirectory(projectDirectory.toAbsolutePath()).toObservable(),

            readProjectFileEvent -> Observable.concat(

                // Write the project file
                CommonTasks.writeFile(
                    Serializers.serialize(readProjectFileEvent.project),
                    projectDirectory.resolve("buckaroo.json").toAbsolutePath(),
                    false).toObservable(),

                // Touch .buckconfig
                CommonTasks.touchFile(projectDirectory.resolve(".buckconfig").toAbsolutePath())
                    .toObservable()
            )
        );
    }

    public static Observable<Event> initWorkingDirectory(final Context ctx) {
        Preconditions.checkNotNull(ctx);
        return init(ctx.fs.getPath(""));
    }
}
