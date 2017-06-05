package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Project;
import com.loopperfect.buckaroo.serialization.Serializers;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;

public final class InitTasks {

    private InitTasks() {

    }

    public static Observable<Event> initWorkingDirectory(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        return Observable.concat(

            // Create an empty project from the working directory
            Single.fromCallable(() -> {
                final Path path = fs.getPath("").toAbsolutePath();
                final Optional<String> projectName = path.getNameCount() > 0 ?
                    Optional.of(path.getName(path.getNameCount() - 1).toString()) :
                    Optional.empty();
                return Project.of(projectName);
            }).flatMap(project ->

                // Write the project file
                CommonTasks.writeFile(
                    Serializers.serialize(project),
                    fs.getPath("buckaroo.json").toAbsolutePath(),
                    false)).toObservable(),

            // Touch .buckconfig
            CommonTasks.touchFile(fs.getPath(".buckconfig").toAbsolutePath())
                .toObservable()
        );
    }
}
