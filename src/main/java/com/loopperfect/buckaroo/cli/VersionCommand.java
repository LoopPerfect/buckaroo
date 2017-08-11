package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.*;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class VersionCommand implements CLICommand {

    private VersionCommand() {

    }

    @Override
    public Function<FileSystem, Observable<Event>> routine() {
        return fs -> Observable.just(Notification.of(Buckaroo.version.encode()));
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof VersionCommand);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public static VersionCommand of() {
        return new VersionCommand();
    }
}
