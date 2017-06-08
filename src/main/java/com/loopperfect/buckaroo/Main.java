package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import com.loopperfect.buckaroo.tasks.LoggingTasks;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main {

    private Main() {

    }

    public static void main(final String[] args) {

        if (args.length == 0) {
            System.out.println("Buck, Buck, Buckaroo! \uD83E\uDD20");
            System.out.println("https://buckaroo.readthedocs.io/");
            return;
        }

        final FileSystem fs = FileSystems.getDefault();

        final String rawCommand = String.join(" ", args);

        // Send the command to the logging server, if present
        LoggingTasks.log(fs, rawCommand).subscribe(); // Ignore any results

        // Parse the command
        final Parser<CLICommand> commandParser = CLIParsers.commandParser;

        try {

            final CLICommand command = commandParser.parse(rawCommand);
            final Observable<Event> task = command.routine().apply(fs);

            final ExecutorService executorService = Executors.newCachedThreadPool();
            final Scheduler scheduler = Schedulers.from(executorService);

            task.subscribeOn(scheduler).subscribe(
                next -> {

                    System.out.println( next.getClass().getSimpleName() );
                },
                error -> {
                    error.printStackTrace();

                    executorService.shutdown();
                    scheduler.shutdown();
                },
                () -> {
                    System.out.println("Done.");

                    executorService.shutdown();
                    scheduler.shutdown();
                });

        } catch (final ParserException e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        }
    }
}
