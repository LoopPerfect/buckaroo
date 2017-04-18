package com.loopperfect.buckaroo.routines;

import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.Optionals;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;

import static com.loopperfect.buckaroo.routines.Routines.*;

public final class Upgrade {

    private Upgrade() {

    }

    public static final IO<Unit> routine = Routines.ensureConfig.flatMap(e -> Optionals.join(
        e,
        i -> IO.println("Error installing default Buckaroo config... ").then(IO.println(i)),
        () -> configFilePath
            .flatMap(path -> IO.println("Reading config from " + path + "... ")
                    .then(IO.value(path)))
            .flatMap(Routines::readConfig)
            .flatMap(readConfigResult -> readConfigResult.join(
                    IO::println,
                    config -> buckarooDirectory.flatMap(path -> continueUntilPresent(
                            config.cookBooks.stream()
                                    .map(cookBook -> IO.println("Upgrading " + cookBook.name + "...")
                                            .then(upgrade(path, cookBook)))
                                    .collect(ImmutableList.toImmutableList()))
                            .flatMap(result -> Optionals.join(
                                    result,
                                    IO::println,
                                    () -> IO.println("Done. "))))))));
}
