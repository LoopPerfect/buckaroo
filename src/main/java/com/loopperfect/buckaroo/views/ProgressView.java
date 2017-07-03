package com.loopperfect.buckaroo.views;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.serialization.DependencyGroupSerializer;
import com.loopperfect.buckaroo.tasks.DependencyInstalledEvent;
import com.loopperfect.buckaroo.tasks.DownloadProgress;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.*;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Stack;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Math.max;
import static java.lang.Math.min;

public final class ProgressView {

    private ProgressView() {

    }

    public static Observable<Either<DependencyInstallationEvent, DependencyInstalledEvent>> installOrInstalled(final Observable<Event> events) {
        return events.filter(x->
            (x instanceof DependencyInstallationEvent) || (x instanceof DependencyInstalledEvent)
        ).map(x -> {
            if (x instanceof DependencyInstallationEvent) {
                return Either.left((DependencyInstallationEvent)x);
            } else {
                return Either.right((DependencyInstalledEvent)x);
            }
        });
    }

    public static int ScoreEvent(Event e) {
        if(e instanceof DependencyInstalledEvent) return 0;
        if(e instanceof DownloadProgress) {
            DownloadProgress p = (DownloadProgress)e;
            return (p.hasKnownContentLength()) ?
                (int)(p.progress()*100) :
                min( (int) (p.downloaded / 1e6 * 30) , 30);
            // if we don't have any ContentLength we can't compute a progress, hence we cant show ProgressBars.
            // Let's prefer ProgressBars over 50% > to Downloaded X bytes notifications
        }

        return 120 -
            max(120, (int)(Instant.now().toEpochMilli() - e.date.toInstant().toEpochMilli())/1000);
        // every event is more important than DownloadProgress but gets less important over time
    }

    public static Observable<Component> progressView(final Observable<Event> events) {

        final Observable<ImmutableList<RecipeIdentifier>> installing = events
            .ofType(DependencyInstallationEvent.class)
            .map(x -> x.progress.getValue0().identifier)
            .distinct()
            .scan(ImmutableList.of(), MoreLists::append);

        final Observable<ImmutableList<RecipeIdentifier>> installed = events
            .ofType(DependencyInstallationEvent.class)
            .map(x->x.progress.getValue1())
            .ofType(DependencyInstalledEvent.class)
            .map(x->x.dependency.identifier)
            .distinct()
            .scan(ImmutableList.of(), MoreLists::append);

        final Observable<Component> total = Observable.combineLatest(
            installing.map(ImmutableList::size).filter(x->x>0),
            installed.map(ImmutableList::size),
            (a, b) -> (Component)Text.of(
                "installed: "+ b + "/" + a,
                Color.YELLOW
        ));


        Comparator<Triplet<DependencyLock, Event, Integer>> comp = Comparator.comparingInt(Triplet::getValue2);

        final Observable<ImmutableList<Event>> installations = events
            .ofType(DependencyInstallationEvent.class)
            .filter(x-> !(x.progress.getValue1() instanceof ResolvedDependenciesEvent))
            .scan(ImmutableMap.of(), (a, b) ->
                MoreMaps.merge(a, ImmutableMap.of(b.progress.getValue0(), b.progress.getValue1()))
            ).map(x-> x.entrySet()
                .stream()
                .map(y -> Triplet.with(
                    (DependencyLock)y.getKey(),
                    (Event)y.getValue(),
                    ScoreEvent((Event)y.getValue()) ))
                .sorted(comp.reversed())
                .limit(3)
                .map(z->DependencyInstallationEvent.of(z.removeFrom2()))
                .collect(toImmutableList()));

        final Observable<Component> progress = installations.map(x->
            x.stream()
                .map(GenericEventRenderer::render)
                .collect(toImmutableList())
        ).map(StackLayout::of);

        return Observable.combineLatest(

            Observable.combineLatest(
                progress,
                total, StackLayout::of
            ).startWith(StackLayout.of()),

            events.scan(0, (a, b) -> a+1)
                .map(x-> (Component)Text.of("performed actions: "+x, Color.BLUE)),

            Observable.combineLatest(
                Observable.just(Instant.now().toEpochMilli()),
                events.map(e->e.date.toInstant().toEpochMilli()),
                (t0, t1) -> (t1-t0)/1000).map(t ->(Component)Text.of("time: "+t+" s",Color.BLUE)),

            events
                .filter(x-> !(x instanceof DependencyInstallationEvent))
                .filter(x-> !(x instanceof ResolvedDependenciesEvent))
                .map(GenericEventRenderer::render),

            StackLayout::of
        );
    }
}
