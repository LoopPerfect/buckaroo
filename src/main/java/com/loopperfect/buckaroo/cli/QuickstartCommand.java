package com.loopperfect.buckaroo.cli;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Quickstart;

public final class QuickstartCommand implements CLICommand {

    private QuickstartCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return Quickstart.routine;
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
