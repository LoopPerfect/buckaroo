package com.loopperfect.buckaroo.cli;

public final class RecipesCommand implements CLICommand {

    private RecipesCommand() {

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
