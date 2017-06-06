package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class RecipesCommand implements CLICommand {

    private RecipesCommand() {

    }

    @Override
    public Function<FileSystem, Observable<Event>> routine() {
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
