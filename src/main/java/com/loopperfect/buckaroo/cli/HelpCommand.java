package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Notification;
import com.loopperfect.buckaroo.Unit;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public final class HelpCommand implements CLICommand {

    private HelpCommand() {
        super();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof HelpCommand;
    }

    @Override
    public Function<FileSystem, Observable<Event>> routine() {
        return fs -> Observable.just(Notification.of("Read the docs at: \n" +
            "https://buckaroo.readthedocs.io/en/latest/cli.html"));
    }

    public static HelpCommand of() {
        return new HelpCommand();
    }
}
