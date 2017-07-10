package com.loopperfect.buckaroo.views;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.DependencyInstallationEvent;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.MoreMaps;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.tasks.DownloadProgress;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.FlowLayout;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;
import io.reactivex.Observable;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public final class StatsView {

    private StatsView() {}

    public static Observable<Component> render(final Observable<Event> observable) {

        Preconditions.checkNotNull(observable);

        final Observable<Event> events = observable.publish().autoConnect(6);

        final Observable<ImmutableMap<RecipeIdentifier, Long>> downloads = events
            .ofType(DependencyInstallationEvent.class)
            .map(e -> e.progress)
            .filter(p -> p.getValue1() instanceof DownloadProgress)
            .scan(ImmutableMap.of(), (a, b) ->
                MoreMaps.merge(a, ImmutableMap.of(
                    b.getValue0().identifier,
                    ((DownloadProgress)b.getValue1()).downloaded)));

        final Observable<Long> downloaded = downloads.map(p ->
            p.values()
                .stream()
                .reduce(0L, (a, b) -> a + b));

        final Observable<Long> timer = Observable.interval(1, TimeUnit.SECONDS)
            .map(x -> 1)
            .scan(0L, (a, b) -> a + b)
            .skipUntil(events.take(1))
            .takeUntil(events.takeLast(1));

        final Observable<Long> eventsCounter = events.scan(0L, (a, b) -> a + 1);

        return Observable.combineLatestDelayError(
            ImmutableList.of(
                timer.map(t -> t + "s ")
                    .startWith(""),
                downloaded.map(d -> d / 1024 + "kb ")
                    .startWith(""),
                eventsCounter.map(c -> c + " events")
                    .startWith("")),
            objects -> StackLayout.of(
                Text.of("Stats: "),
                FlowLayout.of(Arrays.stream(objects)
                    .map(x -> (String)x)
                    .map(x -> Text.of(x, Color.BLUE))
                    .collect(ImmutableList.toImmutableList()))));
    }
}
