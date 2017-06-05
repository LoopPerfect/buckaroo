package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.io.IOContext;
import com.loopperfect.buckaroo.tasks.InitTasks;
import io.reactivex.Observable;

public final class InitCommand implements CLICommand {

    private InitCommand() {

    }

    @Override
    public IO<Unit> routine() {

        return (IOContext context) -> {

            final Observable<Event> task = InitTasks.initWorkingDirectory(context.fs().fileSystem());

            task.subscribe(next -> {
                System.out.println(next);
            }, error -> {
                error.printStackTrace();
            }, () -> {
                System.out.println("Done. ");
            });

            return Unit.of();
        };
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
