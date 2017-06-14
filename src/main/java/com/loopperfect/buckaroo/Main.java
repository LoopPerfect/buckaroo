package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.util.concurrent.SettableFuture;
import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import com.loopperfect.buckaroo.events.FileWriteEvent;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.tasks.DownloadProgress;
import com.loopperfect.buckaroo.tasks.LoggingTasks;
import com.loopperfect.buckaroo.virtualterminal.TerminalBuffer;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.FlowLayout;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

import jdk.nashorn.internal.ir.annotations.Immutable;
import org.fusesource.jansi.AnsiConsole;
import org.javatuples.Pair;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;

import java.awt.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.loopperfect.buckaroo.virtualterminal.TerminalPixel.fill;

public final class Main {

    private Main() {

    }



    public static void main(final String[] args) {

        if (args.length == 0) {
            System.out.println("Buck, Buck, Buckaroo! \uD83E\uDD20");
            System.out.println("https://buckaroo.readthedocs.io/");
            return;
        }

        //AnsiConsole.systemInstall();
        final FileSystem fs = FileSystems.getDefault();

        final String rawCommand = String.join(" ", args);

        // Send the command to the logging server, if present
        LoggingTasks.log(fs, rawCommand).subscribe(); // Ignore any results

        // Parse the command
        final Parser<CLICommand> commandParser = CLIParsers.commandParser;

        try {

            System.out.println("start");
            final CLICommand command = commandParser.parse(rawCommand);
            final Observable<Event> task = command.routine().apply(fs);

            final ExecutorService executorService = Executors.newCachedThreadPool();
            final Scheduler scheduler = Schedulers.from(executorService);

            TerminalBuffer buffer = new TerminalBuffer();


            final Observable<Event> events$ = task.subscribeOn(scheduler);



            final Observable<ReadProjectFileEvent> projectFiles$ = events$
                .filter(e-> e instanceof ReadProjectFileEvent)
                .cast(ReadProjectFileEvent.class);

            final Observable<DownloadProgress> downloads$ = events$
                .filter(e-> e instanceof DownloadProgress)
                .cast(DownloadProgress.class);

                downloads$.subscribe(x->{System.out.println(x);});

            final Observable<ResolvedDependenciesEvent> resolvedDependencies$ = events$
                .filter(e-> e instanceof ResolvedDependenciesEvent)
                .cast(ResolvedDependenciesEvent.class);

            final Observable<FileWriteEvent> fileWrites$ = events$
                .filter(e-> e instanceof FileWriteEvent)
                .cast(FileWriteEvent.class);


            final Observable<Component> projects$ = projectFiles$
                .map(file -> Text.of("reading project file " + file.project.name.orElse("") + " " + file.project.license.orElse("")) );

;
            final Observable<String> writes$  = fileWrites$
                .map(file -> file.path.toString());

            final Observable<ImmutableList<String>>  deps$ = resolvedDependencies$
                .map(deps->deps.dependencies)
                .map(deps->deps.entrySet().stream())
                .map(s->s.map( kv ->
                    kv.getKey().organization.toString()
                    + "/"
                    + kv.getKey().recipe.toString()
                    + "@" + kv.getValue().getValue0().toString()))
                .map(s->s.collect(toImmutableList()));


            final Observable<Component> current$ = Observable.combineLatest(
                projects$,
                writes$
                    .map(w->FlowLayout.of(Text.of("modified: "), Text.of(w, Color.YELLOW))),
                deps$
                    .map(s->s.stream())
                    .map(s->FlowLayout.of(
                         s.map(x->Text.of(x, Color.GREEN)).collect(toImmutableList()))),
                (p,w,d) -> StackLayout.of(
                    Text.of("resolving dependencies", Color.BLUE),
                    p, w,
                    Text.of("resolved deps: "),
                    d
                ));

            final Observable<ImmutableList<Component>> modifiedSummary = writes$.scan(
                ImmutableList.of(),
                (ImmutableList<String> list, String file) -> Streams
                    .concat(list.stream(), Stream.of(file))
                    .collect(toImmutableList())
              ).map( (ImmutableList<String> modifiedFiles) -> ImmutableList.of(
                Text.of("modified Files("+modifiedFiles.size()+") :"),
                FlowLayout.of(
                   Text.of("    "),
                   StackLayout.of(
                       modifiedFiles.stream().map(Text::of).collect(toImmutableList()))
                )));


            final Observable<ImmutableList<Component>> depsSummary = deps$
                .lastElement()
                .toObservable()
                .map(deps -> ImmutableList.of(
                    Text.of("resolved dependencies("+deps.size()+") :"),
                    FlowLayout.of(
                        Text.of("    "),
                        StackLayout.of(deps.stream().map(Text::of).collect(toImmutableList()))
                    )));

            //current$.concatWith(

            Observable<Either<Event, Component>> result =    Observable.merge(

                events$
                    .map(x->{ Either<Event, Component> e = Either.left(x); return e;}),

                Observable
                .combineLatest(
                    modifiedSummary,
                    depsSummary,
                    (m, d) -> new ImmutableList.Builder<Component>()
                        .addAll(m)
                        .addAll(d)
                        .build())
                .lastElement()
                .map(StackLayout::of)
                .map(x->{ Either<Event, Component> e = Either.right(x); return e;})
                .toObservable()

            );


            //)
                result.subscribe(
                    S -> {
                        if(S.left().isPresent()) {
                            System.out.println(S.left().get().getClass().getSimpleName().toString());
                        } else {
                            buffer.flip(S.right().get().render(100));
                        }
                    },
                    error -> {
                        error.printStackTrace();

                        executorService.shutdown();
                        scheduler.shutdown();
                    },
                    () -> {
                        executorService.shutdown();
                        scheduler.shutdown();
                    }
                );

/*
            events$.subscribe(
                next -> {
                    System.out.println( next.getClass().getSimpleName() );
                });
*/


           // events$.connect();
        } catch (final ParserException e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        }
    }
}
