package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;

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
        return context -> {
            context.console().println("Read the docs at: ");
            context.console().println("https://buckaroo.readthedocs.io/en/latest/cli.html");
            return Unit.of();
        };
    }

    public static HelpCommand of() {
        return new HelpCommand();
    }
}
