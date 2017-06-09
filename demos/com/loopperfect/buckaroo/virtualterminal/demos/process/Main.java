package com.loopperfect.buckaroo.virtualterminal.demos.process;

import com.loopperfect.buckaroo.Process2;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.virtualterminal.TerminalBuffer;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;
import com.loopperfect.buckaroo.virtualterminal.components.*;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Created by gaetano on 08/06/17.
 */


public class Main {

    private static class ProgressEvent implements Event {
        final String name;
        final Integer progress;
        ProgressEvent(final String file, final Integer x) {
            name = file;
            progress = x;
        }

        @Override
        public String toString() {
            return (name +" " + progress);
        }
    }


    private static Event createEvent1(final Integer x) {
        return new ProgressEvent("file 1", x);
    }

    private static Event createEvent2(final Integer x) {
        return new ProgressEvent("file 2", x);
    }

    private static Event createEvent3(final Integer x) {
        return new ProgressEvent("file 3", x);
    }

    private static Process2<Event, Integer> count() {
        Observable<Either<Event, Integer>> states =
            Observable.zip(
                Observable.fromArray(0,10,30,50,70,100),
                Observable.interval(300, TimeUnit.MILLISECONDS),
                (obs, timer) -> obs)
            .map(Main::createEvent1)
            .map(Either::left);

        Observable<Either<Event, Integer>> states2 = Observable.zip(
            Observable.fromArray(0,10,80,100, 100),
            Observable.interval(500, TimeUnit.MILLISECONDS),
            (obs, timer) -> obs)
            .map(Main::createEvent2)
            .map(Either::left);

        return Process2.of(
            states.mergeWith(
                states2
            ).concatWith(
                Observable
                    .fromArray(0)
                    .map(Either::right)
            )
        );
    }

    private static Process2<Event, Boolean> countMore(Integer x) {
        Observable<Either<Event,Boolean>> states = Observable.zip(
            Observable.fromArray(x,10,20,30,40,50,75, 100),
            Observable.interval(500, TimeUnit.MILLISECONDS),
            (obs, timer) -> obs)
            .map(Main::createEvent3)
            .map(Either::left);
        return Process2.of(
            states.concatWith(
                Observable
                    .fromArray(true)
                    .map(Either::right)
            )
        );
    }

    public static void main(final String[] args) throws InterruptedException {

        final ExecutorService executorService = Executors.newCachedThreadPool();
        final Scheduler scheduler = Schedulers.from(executorService);

        final TerminalBuffer buffer = new TerminalBuffer();
        final TerminalPixel green = TerminalPixel.of(UnicodeChar.of(' '), Ansi.Color.DEFAULT, Ansi.Color.GREEN);
        final TerminalPixel blue = TerminalPixel.of(UnicodeChar.of(' '), Ansi.Color.DEFAULT, Ansi.Color.BLUE);


        Process2<Event, Boolean> p = Process2.chain(
            count(),
            Main::countMore);

/*
        p.toObservable().subscribe(
            e -> {
                System.out.println(e);
            }
        );*/



        final Observable<ProgressEvent> files = p.states().subscribeOn(scheduler)
            .filter(s-> s instanceof ProgressEvent)
            .map(s-> (ProgressEvent)s);


        final Observable<Set<String>> completedFiles =
            files.filter(f-> f.progress == 100)
                 .scan( new HashSet<String>() , (m, e) ->{ m.add(e.name); return m;});

        final Observable<Map<String,Integer>> incompleteFiles =
            files.filter(f-> true)
                .scan( new HashMap<String,Integer>() , (m, e) ->{ m.put(e.name, e.progress); return m;});



        final Observable<Component> done = completedFiles
            .map( names ->  names.stream().map(name -> (Component)Text.of("completed : "+name)).collect(toImmutableList()) )
            .map( names -> (Component)StackLayout.of(green, names))
            .startWith( StackLayout.of() );

        final Observable<Component> undone = incompleteFiles
            .map( names ->  names.entrySet().stream()
                .filter(x->x.getValue()<100 )
                .map(e ->
                (Component)StackLayout.of(
                    Text.of(e.getKey()) ,
                    ProgressBar.of( e.getValue()/(float)100 )
                )).collect(toImmutableList()) )
            .map( todo -> (Component)StackLayout.of(blue, todo) )
            .startWith( StackLayout.of() );


        AnsiConsole.systemInstall();

        final Observable<Component> xs = Observable.combineLatest(done, undone, (d,u) ->
            StackLayout.of(d, u)
        );


        Object o=0;
        xs.subscribe( c -> {

            synchronized (o) {
                buffer.flip(c.render(100));
            }
        });


        p.result().subscribe(
            x-> {
                System.out.println("done");
                executorService.shutdown();
            }
        );

        p.result().blockingGet();


    }
}
