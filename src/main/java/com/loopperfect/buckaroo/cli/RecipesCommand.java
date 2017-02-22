package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;

public final class RecipesCommand implements CLICommand {

    private RecipesCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return null;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof RecipesCommand);
    }

    public static RecipesCommand of() {
        return new RecipesCommand();
    }
}
