package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import com.loopperfect.buckaroo.views.ProgressView;
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
import org.jparsec.error.ParserException;

import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.loopperfect.buckaroo.EvenMoreFiles.writeFile;

public final class Main {

    private Main() {}

    public static void main(final String[] args) {

        if (args.length == 0) {
            System.out.println("Buck, Buck Buckaroo! \uD83E\uDD20");
            System.out.println("https://buckaroo.readthedocs.io/");
            return;
        }

        // We need to change the default behaviour of Schedulers.io()
        // so that it has a bounded thread-pool.
        // Take at least 2 threads to prevent dead-locks.
        final int threads = 10;


        ExecutorService IOExecutor = Executors.newFixedThreadPool(threads);
        final Scheduler IOScheduler = Schedulers.from(IOExecutor);
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> {
            scheduler.shutdown();
            return IOScheduler;
        });

        final FileSystem fs = FileSystems.getDefault();

        final String rawCommand = String.join(" ", args);

        // Send the command to the logging server, if present
        //LoggingTasks.log(fs, rawCommand).subscribe(); // Ignore any results

        // Parse the command
        final Parser<CLICommand> commandParser = CLIParsers.commandParser;

        try {
            final CLICommand command = commandParser.parse(rawCommand);

            final ExecutorService executorService = Executors.newCachedThreadPool();
            final Scheduler scheduler = Schedulers.from(executorService);

            final Context ctx = Context.of(fs, scheduler);
            final Observable<Event> task = command.routine().apply(ctx);

            final ConnectableObservable<Either<Throwable, Event>> events$ = task
                .observeOn(scheduler)
                .subscribeOn(scheduler)
                .map(e->{
                    final Either<Throwable, Event> r = Either.right(e);
                    return r;
                })
                .onErrorReturn(Either::left)
                .publish();

            final Observable<Component> current$ = events$
                .flatMap(x->{
                    if(x.left().isPresent()) return Observable.error(x.left().get());
                    return Observable.just(x.right().get());
                })
                .compose(ProgressView::progressView)
                .subscribeOn(Schedulers.computation())
                .sample(100, TimeUnit.MILLISECONDS)
                .distinctUntilChanged();

            final Observable<Component> summary$ = events$
                .flatMap(x -> {
                    if(x.left().isPresent()) return Observable.error(x.left().get());
                    return Observable.just(x.right().get());
                })
                .compose(SummaryView::summaryView)
                .subscribeOn(Schedulers.computation())
                .lastElement().toObservable();

            AnsiConsole.systemInstall();
            TerminalBuffer buffer = new TerminalBuffer();

            //TODO: make sure that summary$ emits after current$
            Observable
                .merge(current$, Observable.empty())
                .map(c -> c.render(60))
                .doOnNext(buffer::flip)
                .doOnError(error -> {
                    buffer.flip(
                        StackLayout.of(
                            Text.of("buckaroo failed: "+ error.toString(), Color.RED),
                            Text.of("writing stacktrace to buckaroo-stacktrace.log", Color.YELLOW)
                        ).render(60));
                    writeFile(
                        fs.getPath("").resolve("buckaroo-stacktrace.log"),
                        Arrays.stream(error.getStackTrace())
                            .map(s->s.toString())
                            .reduce(Instant.now().toString()+":", (a, b) -> a+"\n"+b),
                        Charset.defaultCharset(),
                        true);
                }).subscribe(x -> {}, e -> {
                    executorService.shutdown();
                    IOExecutor.shutdown();
                    IOScheduler.shutdown();
                    scheduler.shutdown();
                }, () -> {
                    System.out.println("done...");
                    executorService.shutdown();
                    IOExecutor.shutdown();
                    IOScheduler.shutdown();
                    scheduler.shutdown();
                });

            events$.connect();
        } catch (final ParserException e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        } catch (Throwable e){}
    }
}
