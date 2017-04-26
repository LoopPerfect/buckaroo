package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Help;

public final class HelpCommand implements CLICommand {

    private HelpCommand() {
        super();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof HelpCommand;
    }

    @Override
    public IO<Unit> routine() {
        return Help.routine;
    }

    public static HelpCommand of() {
        return new HelpCommand();
    }
}
