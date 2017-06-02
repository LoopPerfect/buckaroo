package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.tasks.InstallExistingTasks;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import java.nio.file.FileSystem;

public final class InstallExistingCommand implements CLICommand {

    private InstallExistingCommand() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof InstallExistingCommand);
    }

    @Override
    public IO<Unit> routine() {
        return context -> {
            context.console().println("Starting...");

            final FileSystem fs = context.fs().fileSystem();
            final Scheduler scheduler = Schedulers.from(context.executor());

            InstallExistingTasks.installExistingDependenciesInWorkingDirectory(fs)
                .subscribeOn(scheduler)
                .subscribe(
                    next -> {
                        context.console().println(next.toString());
                    },
                    error -> {
                        error.printStackTrace();
                    },
                    () -> {
                        context.console().println("Done. ");
                    });

            return Unit.of();
        };
    }

    public static InstallExistingCommand of() {
        return new InstallExistingCommand();
    }
}
