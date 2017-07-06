package com.loopperfect.buckaroo.views;

import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.tasks.DownloadProgress;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.FlowLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;
import io.reactivex.Observable;

import java.util.concurrent.TimeUnit;

public final class StatsView {

    private StatsView() {}

    public static Observable<Component> render(final Observable<Event> events) {

        Observable<ImmutableMap<RecipeIdentifier, Long>> downloads = events
            .ofType(DependencyInstallationEvent.class)
            .map(e-> e.progress)
            .filter(p -> p.getValue1() instanceof DownloadProgress)
            .scan(ImmutableMap.of(), (a, b) ->
                MoreMaps.merge(a, ImmutableMap.of(
                    b.getValue0().identifier,
                    ((DownloadProgress)b.getValue1()).downloaded)));

        final Observable<Long> downloaded = downloads.map(p ->
            p.values()
                .stream()
                .reduce(0L, (a, b) -> a + b));

        final Observable<Long> timer = Observable
            .interval(1, TimeUnit.SECONDS)
            .map(x -> 1)
            .scan(0L, (a, b) -> a + b)
            .takeUntil(events.last(Notification.of("done")).toObservable());

        final Observable<Long> eventsCounter = events.scan(0L, (a, b) -> a + 1);

        return Observable.combineLatest(
            timer
                .map(t -> t + "s ")
                .startWith(""),
            downloaded
                .map(d -> d/1024 + "kb ")
                .startWith(""),
            eventsCounter
                .map(c -> c + " events")
                .startWith(""),
            (d, t, c) -> FlowLayout.of(
                Text.of("Stats: "),
                Text.of(d, Color.BLUE),
                Text.of(t, Color.BLUE),
                Text.of(c, Color.BLUE)
            )
        ).cast(Component.class);
    }
}
