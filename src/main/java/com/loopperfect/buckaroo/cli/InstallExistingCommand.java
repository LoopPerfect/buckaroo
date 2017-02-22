package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.InstallExisting;

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
    public IO<Unit> routine() {
        return InstallExisting.routine;
    }

    public static InstallExistingCommand of() {
        return new InstallExistingCommand();
    }
}
