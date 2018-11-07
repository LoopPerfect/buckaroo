package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Notification;
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
        return fs -> Observable.just(Notification.of("\n"
                        + "    The C++ package manager that will take you to your happy place\n\n"
                        + "    Usage\n\n"
                        + "      $ buckaroo <options...>\n\n"
                        + "    Options\n\n"
                        + "      init           Init is used to generate a project file in the current\n"
                        + "                     directory\n"
                        + "      quickstart     Similar to init but also generates the necessary\n"
                        + "                     boiler-plate for a new C++ project\n"
                        + "      resolve        Reads the project file in the working directory and runs\n"
                        + "                     the dependency resolution algorithm, storing the results\n"
                        + "                     in the lock file\n"
                        + "      install,   i   Adds and installs dependencies to your project\n"
                        + "      uninstall, u   Remove a dependency from your project\n"
                        + "      upgrade        Upgrades the installed dependencies to the latest\n"
                        + "                     compatible version\n"
                        + "      update         Updates the cook-books installed on your system\n"
                        + "      version        Outputs the version of Buckaroo that is installed\n"
                        + "      help           Outputs this help message\n\n"
                        + "    Examples\n\n"
                        + "      $ buckaroo install google/gtest\n"
                        + "      Stats: 2s 0kb 1091 events\n      Files modified(2):\n      ...\n"
                        + "      $ buckaroo version\n      1.4.1\n\n"
                        + "    For more information read the docs at:\n"
                        + "    https://buckaroo.readthedocs.io/en/latest/cli.html\n"

                )
        );
    }

    public static HelpCommand of() {
        return new HelpCommand();
    }
}
