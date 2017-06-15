package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.buck.BuckFile;
import com.loopperfect.buckaroo.serialization.Serializers;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

public final class QuickstartTasks {

    private QuickstartTasks() {

    }

    private static String helloWorldCpp() throws IOException {
        return Resources.toString(
            Resources.getResource("com.loopperfect.buckaroo/HelloWorld.cpp"),
            Charsets.UTF_8);
    }

    private static Observable<Event> createAppSkeleton(final Path projectDirectory, final Project project) {

        Preconditions.checkNotNull(projectDirectory);
        Preconditions.checkNotNull(project);

        final FileSystem fs = projectDirectory.getFileSystem();
        final Identifier projectIdentifier = project.name.map(StringUtils::escapeStringGitHubStyle)
            .flatMap(Identifier::parse)
            .orElseGet(() -> Identifier.of("my-project"));

        return Observable.concat(ImmutableList.of(

            // Write the project file
            CommonTasks.writeFile(
                Serializers.serialize(project),
                fs.getPath("buckaroo.json").toAbsolutePath(),
                false)
                .toObservable(),

            // Touch the .buckconfig
            CommonTasks.touchFile(fs.getPath(projectDirectory.toString(), ".buckconfig"))
                .toObservable(),

            // Generate an empty BUCKAROO_DEPS
            CommonTasks.writeFile(
                CommonTasks.generateBuckarooDeps(ImmutableList.of()),
                fs.getPath(projectDirectory.toString(), "BUCKAROO_DEPS"),
                false)
                .toObservable(),

            // Create the project directories
            CommonTasks.createDirectory(fs.getPath(projectDirectory.toString(), projectIdentifier.name))
                .toObservable(),

            CommonTasks.createDirectory(fs.getPath(projectDirectory.toString(), projectIdentifier.name, "src"))
                .toObservable(),

            CommonTasks.createDirectory(fs.getPath(projectDirectory.toString(), projectIdentifier.name, "include"))
                .toObservable(),

            // Write the Hello World cpp file
            Single.fromCallable(QuickstartTasks::helloWorldCpp).flatMap(content -> CommonTasks.writeFile(
                content,
                fs.getPath(projectDirectory.toString(), projectIdentifier.name, "src", "main.cpp"),
                false))
                .toObservable(),

            // Generate the BUCK file
            Single.fromCallable(() -> Either.orThrow(BuckFile.generate(projectIdentifier)))
                .flatMapObservable(content -> Observable.concat(
                    CommonTasks.writeFile(
                        content,
                        fs.getPath(projectDirectory.toString(), "BUCK"),
                        false).toObservable(),
                    Observable.just(Notification.of("buck run :" + projectIdentifier))
                ))
        ));
    }

    public static Observable<Event> quickstartInWorkingDirectory(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        return InitTasks.generateProjectForDirectory(fs.getPath("").toAbsolutePath())
            .flatMapObservable(event -> createAppSkeleton(fs.getPath("").toAbsolutePath(), event.project));
    }
}
