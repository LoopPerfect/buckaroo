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
import java.util.Optional;

public final class QuickstartTasks {

    private QuickstartTasks() {

    }

    private static String helloWorldCpp() throws IOException {
        return Resources.toString(
            Resources.getResource("com.loopperfect.buckaroo/HelloWorld.cpp"),
            Charsets.UTF_8);
    }

    private static String defaultBuckConfig() throws IOException {
        return Resources.toString(
                Resources.getResource("com.loopperfect.buckaroo/DefaultBuckConfig.ini"),
                Charsets.UTF_8);
    }

    private static Observable<Event> createAppSkeleton(final Path projectDirectory, final Project project) {

        Preconditions.checkNotNull(projectDirectory);
        Preconditions.checkNotNull(project);

        final FileSystem fs = projectDirectory.getFileSystem();
        final Identifier projectIdentifier = project.name.map(StringUtils::escapeStringGitHubStyle)
            .flatMap(Identifier::parse)
            // The project name "buck" interferes with the BUCK file
            .flatMap(x -> x.name.equalsIgnoreCase("buck") ?
                Optional.empty() : Optional.of(x))
            .orElseGet(() -> Identifier.of("my-project"));

        return Observable.concat(ImmutableList.of(

            // Write the project file
            CommonTasks.writeFile(
                Serializers.serialize(project),
                fs.getPath("buckaroo.json").toAbsolutePath(),
                false)
                .toObservable()
                .cast(Event.class)
                .onErrorReturnItem(Notification.of("buckaroo.json already exists!")),

            // Write the default .buckconfig
            Single.fromCallable(QuickstartTasks::defaultBuckConfig)
                .flatMap(content ->
                    CommonTasks.writeFile(
                            content,
                            fs.getPath(".buckconfig").toAbsolutePath(),
                            false))
                .toObservable()
                .cast(Event.class)
                .onErrorReturnItem(Notification.of(".buckconfig already exists!")),

            // Generate an empty BUCKAROO_DEPS
            Single.fromCallable(() -> CommonTasks.generateBuckarooDeps(ImmutableList.of()))
                .flatMap(content ->
                    CommonTasks.writeFile(
                        content,
                        fs.getPath(projectDirectory.toString(), "BUCKAROO_DEPS"),
                        false))
                .toObservable()
                .cast(Event.class)
                .onErrorReturnItem(Notification.of("BUCKAROO_DEPS already exists!")),

            // Create the project directories
            CommonTasks.createDirectory(fs.getPath(projectDirectory.toString(), projectIdentifier.name))
                .toObservable(),

            CommonTasks.createDirectory(fs.getPath(projectDirectory.toString(), projectIdentifier.name, "src"))
                .toObservable(),

            CommonTasks.createDirectory(fs.getPath(projectDirectory.toString(), projectIdentifier.name, "include"))
                .toObservable(),

            CommonTasks.createDirectory(fs.getPath(projectDirectory.toString(), projectIdentifier.name, "detail"))
                .toObservable(),

            // Write the Hello World cpp file
            Single.fromCallable(QuickstartTasks::helloWorldCpp).flatMap(content -> CommonTasks.writeFile(
                content,
                fs.getPath(projectDirectory.toString(), projectIdentifier.name, "apps", "main.cpp"),
                false))
                .toObservable()
                .cast(Event.class)
                .onErrorReturnItem(Notification.of("apps/main.cpp already exists!")),

            // Generate the BUCK file
            Single.fromCallable(() -> Either.orThrow(BuckFile.generate(projectIdentifier)))
                .flatMapObservable(content -> Observable.concat(
                    CommonTasks.writeFile(
                        content,
                        fs.getPath(projectDirectory.toString(), "BUCK"),
                        false).toObservable()
                        .cast(Event.class)
                        .onErrorReturnItem(Notification.of("BUCK-file already exists!")),
                    Observable.just(Notification.of("buck run :main"))
                ))
        ));
    }

    public static Observable<Event> quickstartInWorkingDirectory(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        return InitTasks.generateProjectForDirectory(fs.getPath("").toAbsolutePath())
            .flatMapObservable(event -> createAppSkeleton(fs.getPath("").toAbsolutePath(), event.project));
    }
}
