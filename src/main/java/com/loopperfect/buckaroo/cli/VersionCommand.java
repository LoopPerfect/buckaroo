package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Buckaroo;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;

public final class VersionCommand implements CLICommand {

    private VersionCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return context -> {
            context.console().println(Buckaroo.version.toString());
            return Unit.of();
        };
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof VersionCommand);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public static VersionCommand of() {
        return new VersionCommand();
    }
}
