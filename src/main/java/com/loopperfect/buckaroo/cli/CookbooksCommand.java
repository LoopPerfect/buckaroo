package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.Event;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class CookbooksCommand implements CLICommand {

    private CookbooksCommand() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof CookbooksCommand);
    }

    @Override
    public Function<Context, Observable<Event>> routine() {
        return null;
    }

    public static CookbooksCommand of() {
        return new CookbooksCommand();
    }
}
