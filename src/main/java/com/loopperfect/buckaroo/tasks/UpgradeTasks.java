package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.Event;
import io.reactivex.Observable;

import java.nio.file.FileSystem;

public final class UpgradeTasks {

    private UpgradeTasks() {

    }

    public static Observable<Event> upgradeInWorkingDirectory(final Context ctx) {

        Preconditions.checkNotNull(ctx);

        return Observable.concat(
            ResolveTasks.resolveDependenciesInWorkingDirectory(ctx),
            InstallExistingTasks.installExistingDependenciesInWorkingDirectory(ctx));
    }
}
