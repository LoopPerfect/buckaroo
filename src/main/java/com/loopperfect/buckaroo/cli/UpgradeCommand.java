package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Upgrade;

public final class UpgradeCommand implements CLICommand {

    private UpgradeCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return Upgrade.routine;
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
