package com.loopperfect.buckaroo.cli;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.tasks.QuickstartTasks;
import io.reactivex.Observable;

public final class QuickstartCommand implements CLICommand {

    private QuickstartCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return context -> {

            final Observable<Event> task = QuickstartTasks.quickstartInWorkingDirectory(context.fs().fileSystem());

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
