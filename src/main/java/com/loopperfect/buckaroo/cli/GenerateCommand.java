package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.Event;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class GenerateCommand implements CLICommand {

    private GenerateCommand() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof GenerateCommand);
    }

    @Override
    public Function<Context, Observable<Event>> routine() {
        return null;
    }

    public static GenerateCommand of() {
        return new GenerateCommand();
    }
}
