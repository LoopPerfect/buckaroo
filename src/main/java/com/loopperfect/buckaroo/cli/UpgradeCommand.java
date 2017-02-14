package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RemoteCookBook;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Routines;

public final class UpgradeCommand implements CLICommand {

    private UpgradeCommand() {

    }

    @Override
    public IO<Unit> routine() {
        final RemoteCookBook cookBook = RemoteCookBook.of(
                Identifier.of("buckaroo-recipes-test"),
                "git@github.com:njlr/buckaroo-recipes-test.git");
        return Routines.upgrade(cookBook);
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
