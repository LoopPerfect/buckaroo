package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Notification;
import io.reactivex.Observable;

import java.nio.file.FileSystem;

public final class UpgradeTasks {

    private UpgradeTasks() {

    }

    public static Observable<Event> upgradeInWorkingDirectory(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        return Observable.concat(
            ResolveTasks.resolveDependenciesInWorkingDirectory(fs),
            InstallExistingTasks.installExistingDependenciesInWorkingDirectory(fs),
            Observable.just(Notification.of("Upgrade complete")));
    }
}
