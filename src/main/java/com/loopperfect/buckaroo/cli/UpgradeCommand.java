package com.loopperfect.buckaroo.cli;

public final class UpgradeCommand implements CLICommand {

    private UpgradeCommand() {

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
