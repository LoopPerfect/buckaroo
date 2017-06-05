package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.tasks.UpgradeTasks;
import io.reactivex.Observable;

public final class UpgradeCommand implements CLICommand {

    private UpgradeCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return context -> {

            final Observable<Event> task = UpgradeTasks.upgradeInWorkingDirectory(context.fs().fileSystem());

            task.subscribe(
                next -> System.out.println(next),
                error -> error.printStackTrace(),
                () -> System.out.println("Done. "));

            return Unit.of();
        };
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this || (obj != null && obj instanceof UpgradeCommand);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public static UpgradeCommand of() {
        return new UpgradeCommand();
    }
}
