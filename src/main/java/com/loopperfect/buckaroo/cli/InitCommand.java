package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.tasks.InitTasks;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class InitCommand implements CLICommand {

    private InitCommand() {

    }

    @Override
    public Function<FileSystem, Observable<Event>> routine() {
        return InitTasks::initWorkingDirectory;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof InitCommand);
    }

    public static InitCommand of() {
        return new InitCommand();
    }
}
