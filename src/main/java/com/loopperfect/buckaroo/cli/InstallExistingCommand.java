package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.tasks.InstallExistingTasks;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

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
    public Function<Context, Observable<Event>> routine() {
        return InstallExistingTasks::installExistingDependenciesInWorkingDirectory;
    }

    public static InstallExistingCommand of() {
        return new InstallExistingCommand();
    }
}
