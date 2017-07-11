package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import com.loopperfect.buckaroo.sources.RecipeNotFoundException;
import com.loopperfect.buckaroo.tasks.LoggingTasks;
import com.loopperfect.buckaroo.views.GenericEventRenderer;
import com.loopperfect.buckaroo.views.ProgressView;
import com.loopperfect.buckaroo.views.StatsView;
import com.loopperfect.buckaroo.views.SummaryView;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.TerminalBuffer;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.ListLayout;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.fusesource.jansi.AnsiConsole;
import org.jparsec.Parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.ImmutableList.toImmutableList;

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
        final int threads = Math.min(Math.max(2, Runtime.getRuntime().availableProcessors() * 2), 12);

        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final Scheduler scheduler = Schedulers.from(executor);
        final FileSystem fs = FileSystems.getDefault();

        final String rawCommand = String.join(" ", args);

        final CountDownLatch loggingLatch = new CountDownLatch(1);
        final CountDownLatch taskLatch = new CountDownLatch(1);

        // Send the command to the logging server, if present
        LoggingTasks.log(fs, rawCommand).subscribe(
            next -> {
                // Do nothing
            },
            error -> {
                // Do nothing
                loggingLatch.countDown();
            },
            () -> {
                // Do nothing
                loggingLatch.countDown();
            });

        // Parse the command
        final Parser<CLICommand> commandParser = CLIParsers.commandParser;

        try {
            final CLICommand command = commandParser.parse(rawCommand);

            final Observable<Event> task = command.routine().apply(fs)
                .subscribeOn(scheduler);

            final Observable<Either<Throwable, Event>> errorOrEvent = task
                .subscribeOn(scheduler)
                .map(x -> {
                    final Either<Throwable, Event> e = Either.right(x);
                    return e;
                }).onErrorReturn(x -> {
                    final Either<Throwable, Event> e = Either.left(x);
                    return e;
                });

            final Observable<Component> components = errorOrEvent
                .publish(upstream -> {
                        Observable<Component> errors  = upstream.filter(Either::isLeft).map(x->x.left().get()).cache()
                            .take(1).flatMap(Observable::error).cast(Component.class);
                        return upstream.takeUntil(errors).filter(Either::isRight).map(e -> e.right().get()).compose(u ->
                            Observable.combineLatest(
                                    ProgressView.render(u)
                                        .startWith(StackLayout.of())
                                        .subscribeOn(Schedulers.computation())
                                        .concatWith(Observable.just(StackLayout.of())),
                                    StatsView.render(u)
                                        .subscribeOn(Schedulers.computation())
                                        .skip(300, TimeUnit.MILLISECONDS)
                                        .takeUntil(upstream.lastElement().toObservable())
                                        .startWith(StackLayout.of()),
                                    SummaryView.render(u)
                                        .takeLast(1)
                                        .startWith(StackLayout.of()),
                                (x, y, z) -> (Component) StackLayout.of(x, y, z))
                        ).concatWith(errors);
                })
                .subscribeOn(Schedulers.computation())
                .sample(100, TimeUnit.MILLISECONDS, true)
                .distinctUntilChanged()
                .doOnNext(x->{ System.gc(); });

            AnsiConsole.systemInstall();

            final TerminalBuffer buffer = new TerminalBuffer();

            components
                .map(x -> x.render(TERMINAL_WIDTH))
                .subscribe(
                    buffer::flip,
                    error -> {
                        executor.shutdown();
                        scheduler.shutdown();
                        taskLatch.countDown();

                        if(error instanceof RecipeNotFoundException) {
                            final RecipeNotFoundException notFound = (RecipeNotFoundException)error;

                            final ImmutableList<Component> candidates =
                                Streams.stream(notFound.source.findCandidates(notFound.identifier))
                                    .limit(3)
                                    .map(GenericEventRenderer::render)
                                    .collect(toImmutableList());

                            if(candidates.size()>0) {
                                buffer.flip(
                                    StackLayout.of(
                                        Text.of("Error! \n" + error.toString(), Color.RED),
                                        Text.of("Maybe you meant to install one of the following?"),
                                        ListLayout.of(candidates)).render(TERMINAL_WIDTH));
                            } else {
                                buffer.flip(Text.of("Error! \n" + error.toString(), Color.RED).render(TERMINAL_WIDTH));
                            }
                            return;
                        }

                        if(error instanceof CookbookUpdateException) {
                            buffer.flip(Text.of("Error! \n" + error.toString(), Color.RED).render(TERMINAL_WIDTH));
                            return;
                        }

                        try {
                            EvenMoreFiles.writeFile(
                                fs.getPath("buckaroo-stacktrace.log"),
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
                                render(TERMINAL_WIDTH));
                    },
                    () -> {
                        executor.shutdown();
                        scheduler.shutdown();
                        taskLatch.countDown();
                    });

            taskLatch.await();
            loggingLatch.await(1000L, TimeUnit.MILLISECONDS);

        } catch (final Throwable e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        }
    }
}
