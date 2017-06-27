package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import com.loopperfect.buckaroo.tasks.LoggingTasks;
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
import org.jparsec.error.ParserException;

import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.loopperfect.buckaroo.views.ProgressView.progressView;
import static com.loopperfect.buckaroo.views.SummaryView.summaryView;

public final class Main {

    private static final int TERMINAL_WIDTH = 60;

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
        final int threads = Math.min(Math.max(2, Runtime.getRuntime().availableProcessors() * 2), 12);

        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final Scheduler scheduler = Schedulers.from(executor);
        final Context context = Context.of(FileSystems.getDefault(), scheduler);

        RxJavaPlugins.setIoSchedulerHandler(oldScheduler -> {

            // Shutdown the old scheduler
            oldScheduler.shutdown();

            // Use the scheduler from the context
            return context.scheduler;
        });

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

            final Observable<Event> task = command.routine().apply(context);

            final ConnectableObservable<Event> events = task
                .observeOn(scheduler)
                .subscribeOn(scheduler)
                .publish();

            final Observable<Component> current = progressView(events)
                .subscribeOn(Schedulers.computation())
                .sample(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged();

            final Observable<Component> summary = summaryView(events)
                .subscribeOn(Schedulers.computation())
                .takeLast(1);

            AnsiConsole.systemInstall();

            final TerminalBuffer buffer = new TerminalBuffer();

            Observable.combineLatest(
                summary.startWith(StackLayout.of()),
                current.startWith(StackLayout.of()),
                (x, y) -> StackLayout.of(x, y))
                .map(c -> c.render(TERMINAL_WIDTH))
                .subscribe(
                    buffer::flip,
                    error -> {
                        buffer.flip(
                            StackLayout.of(
                                Text.of("Buckaroo hit an error: \n" + error.toString(), Color.RED),
                                Text.of("Writing the stack-trace to buckaroo-stacktrace.log. ", Color.YELLOW)).
                                render(60));
                        EvenMoreFiles.writeFile(
                            context.fs.getPath("").resolve("buckaroo-stacktrace.log"),
                            Arrays.stream(error.getStackTrace())
                                .map(StackTraceElement::toString)
                                .reduce(Instant.now().toString() + ": ", (a, b) -> a + "\n" + b),
                            Charset.defaultCharset(),
                            true);
                        executor.shutdown();
                        scheduler.shutdown();
                    },
                    () -> {
                        executor.shutdown();
                        scheduler.shutdown();
                    });

            // Trigger the actual execution of the observable graph.
            events.connect();

        } catch (final ParserException e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        }
    }
}
