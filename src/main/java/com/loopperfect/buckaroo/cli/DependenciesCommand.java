package com.loopperfect.buckaroo.cli;

import com.google.common.base.MoreObjects;
import com.loopperfect.buckaroo.Event;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class DependenciesCommand implements CLICommand {

    private DependenciesCommand() {

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
        return this == obj || (obj != null && obj instanceof DependenciesCommand);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .toString();
    }

    public static DependenciesCommand of() {
        return new DependenciesCommand();
    }
}
