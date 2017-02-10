package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Routines;

public final class InitCommand implements CLICommand {

    private InitCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return Routines.createProjectSkeleton;
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
