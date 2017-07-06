package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.tasks.UpgradeTasks;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class UpgradeCommand implements CLICommand {

    private UpgradeCommand() {

    }

    @Override
    public Function<FileSystem, Observable<Event>> routine() {
        return UpgradeTasks::upgradeInWorkingDirectory;
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
