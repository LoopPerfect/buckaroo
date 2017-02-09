package com.loopperfect.buckaroo.cli;

public final class InitCommand implements CLICommand {

    private InitCommand() {

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
