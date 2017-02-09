package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.BuckarooException;
import com.loopperfect.buckaroo.Routine;

public final class UpgradeCommand implements CLICommand {

    private UpgradeCommand() {

    }

    @Override
    public Routine routine() {
        return () -> {
            throw new BuckarooException("Not implemented yet! ");
        };
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
