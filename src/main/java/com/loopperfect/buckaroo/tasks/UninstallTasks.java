package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.nio.file.Path;

public final class UninstallTasks {

    private UninstallTasks() {
        super();
    }

    public static Observable<Event> uninstallInWorkingDirectory(final FileSystem fs,
        final ImmutableList<PartialRecipeIdentifier> identifiers) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(identifiers);

        final Path projectFilePath = fs.getPath("buckaroo.json").toAbsolutePath();

        return CommonTasks.readProjectFile(projectFilePath).toObservable()
            .compose(new PublishAndMergeTransformer<ReadProjectFileEvent, Event, Event>(readProjectFileEvent -> {

                final Project project = readProjectFileEvent.project;

                final ImmutableList<Dependency> toRemove = project.dependencies.entries()
                    .stream()
                    .filter(x -> identifiers.stream().anyMatch(y -> y.isSatisfiedBy(x.project)))
                    .distinct()
                    .collect(ImmutableList.toImmutableList());

                if (toRemove.isEmpty()) {
                    return Observable.just(Notification.of("No dependencies to remove. "));
                }

                final Project nextProject = project.removeDependencies(toRemove);

                return Observable.concat(

                    // Write the new project file
                    CommonTasks.writeFile(Serializers.serialize(nextProject), projectFilePath, true)
                        .toObservable()
                        .cast(Event.class),

                    // Upgrade
                    UpgradeTasks.upgradeInWorkingDirectory(fs)
                );
            }));
    }
}
