package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.events.InitProjectEvent;
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

    public static Single<InitProjectEvent> generateProjectForDirectory(final Path path) {

        Preconditions.checkNotNull(path);

        return Single.fromCallable(() -> {
            final Optional<String> projectName = path.getNameCount() > 0 ?
                Optional.of(path.getName(path.getNameCount() - 1).toString()) :
                Optional.empty();
            return InitProjectEvent.of(Project.of(projectName));
        });
    }

    public static Observable<Event> init(final Path projectDirectory) {

        Preconditions.checkNotNull(projectDirectory);

        // Create an empty project from the working directory
        return generateProjectForDirectory(projectDirectory.toAbsolutePath()).flatMapObservable(

            initProjectEvent -> Observable.concat(
                Observable.just((Event)initProjectEvent),
                // Write the project file
                CommonTasks.writeFile(
                    Serializers.serialize(initProjectEvent.project),
                    projectDirectory.resolve("buckaroo.json").toAbsolutePath(),
                    false)
                    .toObservable()
                    .cast(Event.class)
                    .onErrorReturnItem(
                        Notification.of("buckaroo.json already exists!")),

                // Touch .buckconfig
                CommonTasks.touchFile(projectDirectory.resolve(".buckconfig").toAbsolutePath())
                    .toObservable(),

                Observable.just(Notification.of("initialization complete"))
            )
        );
    }

    public static Observable<Event> initWorkingDirectory(final Context ctx) {
        Preconditions.checkNotNull(ctx);
        return init(ctx.fs.getPath(""));
    }
}
