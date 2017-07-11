package com.loopperfect.buckaroo;

import com.google.common.util.concurrent.SettableFuture;
import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import com.loopperfect.buckaroo.tasks.LoggingTasks;
import com.loopperfect.buckaroo.views.ProgressView;
import com.loopperfect.buckaroo.views.StatsView;
import com.loopperfect.buckaroo.views.SummaryView;
import com.loopperfect.buckaroo.virtualterminal.TerminalBuffer;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.fusesource.jansi.AnsiConsole;
import org.jparsec.Parser;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.loopperfect.buckaroo.ErrorHandler.handleErrors;

public final class Main {

    public static final int TERMINAL_WIDTH = 80;

    private Main() {}

    public static void main(final String[] args) {

        if (args.length == 0) {
            System.out.println("Buck, Buck, Buckaroo! \uD83E\uDD20");
            System.out.println("https://buckaroo.readthedocs.io/");
            return;
        }

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

            final Observable<Event> task = command.routine().apply(fs);


            // our renderers can't handle errors properly let's eitherize the stream
            // and split it to two stream in publish.
            final Observable<Either<Throwable, Event>> errorOrEvent = task
                .map(Either::<Throwable, Event>right)
                .onErrorReturn(Either::<Throwable, Event>left);

            final Observable<Component> components = errorOrEvent
                .publish(upstream -> {
                        // It's crucial that we don't subscribe multiple times to our event emitters as they do I/O.
                        Observable<Component> errors  = upstream
                            .filter(Either::isLeft)
                            .map(x->x.left().get())
                            .cache() // upstream is a hot observable, we want make sure we don't lose the error
                            .flatMap(Observable::error)
                            .cast(Component.class);

                        return upstream
                            .takeUntil(errors) // stop when an error occurs, takeUntil also propagates the error further down the chain.
                            .filter(Either::isRight)
                            .map(e -> e.right().get())
                            .compose(u -> Observable.combineLatest(
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
                        );
                })
                .subscribeOn(Schedulers.computation())
                .sample(100, TimeUnit.MILLISECONDS, true) //most terminals can't handle more than 10fps
                .distinctUntilChanged();

            AnsiConsole.systemInstall();
            final TerminalBuffer buffer = new TerminalBuffer();

            final SettableFuture<Throwable> errorF = SettableFuture.create();

            components
                .map(x -> x.render(TERMINAL_WIDTH))
                .subscribe(
                    str -> {
                        buffer.flip(str);
                        // This is only a hint for garbage collection.
                        // This is an opportune moment for collection as we accumulated a lot of Components.
                        // The next render wont happen in the next 100ms anyway...
                        System.gc();
                    },
                    error -> {
                        errorF.set(error);
                        taskLatch.countDown();
                    },
                    () -> {
                        taskLatch.countDown();
                    });

            //makes sure we don't exit main before we sucessfully unsubscribed from the eventstream
            taskLatch.await();

            if (errorF.isDone()) {
                // It turns out rxJava does not guaranty that onNext wont be called onError in a multithreaded enviroment.
                // Lets store the error in a future, wait untill all events stopped firing and render a message to the user.
                handleErrors(errorF.get(), buffer, fs);
            }

            try {
                loggingLatch.await(1000L, TimeUnit.MILLISECONDS);
            } catch (Throwable ignored) {

            }

        } catch (final Throwable e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        }
    }
}
