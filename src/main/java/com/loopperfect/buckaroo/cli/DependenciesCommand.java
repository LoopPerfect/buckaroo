package com.loopperfect.buckaroo.cli;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Routines;

public final class DependenciesCommand implements CLICommand {

    private DependenciesCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return Routines.listDependencies;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof DependenciesCommand);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .toString();
    }

    public static DependenciesCommand of() {
        return new DependenciesCommand();
    }
}
