package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import com.loopperfect.buckaroo.tasks.LoggingTasks;
import com.loopperfect.buckaroo.views.ProgressView;
import com.loopperfect.buckaroo.views.StatsView;
import com.loopperfect.buckaroo.views.SummaryView;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.TerminalBuffer;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import org.fusesource.jansi.AnsiConsole;
import org.jparsec.Parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Main {

    private static final int TERMINAL_WIDTH = 80;

    private Main() {}

    public static void main(final String[] args) {

        if (args.length == 0) {
            System.out.println("Buck, Buck, Buckaroo! \uD83E\uDD20");
            System.out.println("https://buckaroo.readthedocs.io/");
            return;
        }

        // We need to change the default behaviour of Schedulers.io()
        // so that it has a bounded thread-pool.
        // Take at least 2 threads to prevent dead-locks.
        // Take at most 12 to prevent too many downloads happening in parallel
         final int threads = 10;

        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final Scheduler scheduler = Schedulers.from(executor);
        final Context context = Context.of(FileSystems.getDefault(), scheduler);
/*
        RxJavaPlugins.setIoSchedulerHandler(oldScheduler -> {

            // Shutdown the old scheduler
            oldScheduler.shutdown();

            // Use the scheduler from the context
            return context.scheduler;
        });
*/
        final String rawCommand = String.join(" ", args);

        // Send the command to the logging server, if present
        LoggingTasks.log(context.fs, rawCommand).subscribe(
            next -> {
                // Do nothing
            },
            error -> {
                // Do nothing
            },
            () -> {
                // Do nothing
            });

        // Parse the command
        final Parser<CLICommand> commandParser = CLIParsers.commandParser;

        try {
            final CLICommand command = commandParser.parse(rawCommand);

            final Observable<Event> task = command.routine().apply(context)
                .observeOn(scheduler)
                .subscribeOn(scheduler);

            final Observable<Component> components = task
                .observeOn(scheduler)
                .subscribeOn(scheduler)
                .publish(upstream -> Observable.combineLatest(
                    ProgressView.render(upstream)
                        .subscribeOn(Schedulers.computation())
                        .startWith(StackLayout.of())
                        .concatWith(Observable.just(StackLayout.of())),
                    StatsView.render(upstream)
                        .subscribeOn(Schedulers.computation()),
                    SummaryView.render(upstream)
                        .takeLast(1)
                        .startWith(StackLayout.of())
                        .subscribeOn(Schedulers.computation()),
                    (x, y, z) -> (Component)StackLayout.of(x, y, z)))
                .subscribeOn(Schedulers.computation())
                .sample(100, TimeUnit.MILLISECONDS, true)
                .distinctUntilChanged();

            AnsiConsole.systemInstall();

            final TerminalBuffer buffer = new TerminalBuffer();

            final CountDownLatch latch = new CountDownLatch(1);

            components
                .map(c -> c.render(TERMINAL_WIDTH))
                .subscribe(
                    buffer::flip,
                    error -> {
                        try {
                            EvenMoreFiles.writeFile(
                                context.fs.getPath("buckaroo-stacktrace.log"),
                                Arrays.stream(error.getStackTrace())
                                    .map(StackTraceElement::toString)
                                    .reduce(Instant.now().toString() + ": ", (a, b) -> a + "\n" + b),
                                Charset.defaultCharset(),
                                true);
                        } catch (final IOException ignored) {

                        }
                        buffer.flip(
                            StackLayout.of(
                                Text.of("Error! \n" + error.toString(), Color.RED),
                                Text.of("The stacktrace was written to buckaroo-stacktrace.log. ", Color.YELLOW)).
                                render(60));
                        executor.shutdown();
                        scheduler.shutdown();
                        latch.countDown();
                    },
                    () -> {
                        executor.shutdown();
                        scheduler.shutdown();
                        latch.countDown();
                    });

            latch.await();

        } catch (final Throwable e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        }
    }
}
