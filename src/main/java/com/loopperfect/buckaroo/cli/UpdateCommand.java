package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.tasks.UpdateTasks;
import io.reactivex.Observable;

public final class UpdateCommand implements CLICommand {

    private UpdateCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return context -> {
            final Observable<Event> observable = UpdateTasks.updateCookbooks(context.fs().fileSystem());

            observable.subscribe(next -> {
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
        return (obj != null) && (obj instanceof UpdateCommand);
    }

    public static UpdateCommand of() {
        return new UpdateCommand();
    }
}
