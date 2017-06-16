package com.loopperfect.buckaroo.cli;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.tasks.QuickstartTasks;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class QuickstartCommand implements CLICommand {

    private QuickstartCommand() {

    }

    @Override
    public Function<Context, Observable<Event>> routine() {
        return QuickstartTasks::quickstartInWorkingDirectory;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof QuickstartCommand;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }

    public static QuickstartCommand of() {
        return new QuickstartCommand();
    }
}
