package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.tasks.ResolveTasks;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class ResolveCommand implements CLICommand {

    private ResolveCommand() {

    }

    @Override
    public Function<FileSystem, Observable<Event>> routine() {
        return ResolveTasks::resolveDependenciesInWorkingDirectory;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof ResolveCommand;
    }

    public static ResolveCommand of() {
        return new ResolveCommand();
    }
}
