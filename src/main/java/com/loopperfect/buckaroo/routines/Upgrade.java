package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.GitCommit;
import com.loopperfect.buckaroo.Optionals;
import com.loopperfect.buckaroo.RemoteCookBook;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.util.Optional;

import static com.loopperfect.buckaroo.routines.Routines.buckarooDirectory;
import static com.loopperfect.buckaroo.routines.Routines.configFilePath;
import static com.loopperfect.buckaroo.routines.Routines.continueUntilPresent;

public final class Upgrade {

    private Upgrade() {

    }

    private static IO<Optional<IOException>> upgrade(
            final String buckarooDirectory, final RemoteCookBook cookBook) {
        Preconditions.checkNotNull(buckarooDirectory);
        Preconditions.checkNotNull(cookBook);
        final String cookBookPath = buckarooDirectory + "/" + cookBook.name;
        return Routines.ensureCheckout(cookBookPath, GitCommit.of(cookBook.url, "master"))
                .map(x -> x.left().map(IOException::new));
    }

    public static final IO<Unit> routine = configFilePath
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
                                    () -> IO.println("Done. "))))));
}
