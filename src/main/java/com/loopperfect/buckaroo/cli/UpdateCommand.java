package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.tasks.UpdateTasks;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class UpdateCommand implements CLICommand {

    private UpdateCommand() {

    }

    @Override
    public Function<Context, Observable<Event>> routine() {
        return UpdateTasks::updateCookbooks;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj != null) && (obj instanceof UpdateCommand);
    }

    public static UpdateCommand of() {
        return new UpdateCommand();
    }
}
