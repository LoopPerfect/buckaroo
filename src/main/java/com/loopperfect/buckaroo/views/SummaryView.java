package com.loopperfect.buckaroo.views;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.MoreLists;
import com.loopperfect.buckaroo.Notification;
import com.loopperfect.buckaroo.events.TouchFileEvent;
import com.loopperfect.buckaroo.events.WriteFileEvent;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.ListLayout;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;
import io.reactivex.Observable;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class SummaryView {

    private SummaryView() {

    }

    public static Observable<Component> render(final Observable<Event> observable) {

        Preconditions.checkNotNull(observable);

        final Observable<Event> events = observable.publish().autoConnect(3);

        // 1. modifiedSummary
        final Observable<ImmutableList<Component>> modifiedSummary = events
            .filter(x -> x instanceof WriteFileEvent || x instanceof TouchFileEvent)
            .scan(
                ImmutableList.of(),
                (ImmutableList<Event> list, Event event) -> Streams
                    .concat(list.stream(), Stream.of(event))
                    .collect(toImmutableList()))
            .map((ImmutableList<Event> modifiedFiles) -> modifiedFiles.isEmpty() ?
                ImmutableList.of() :
                ImmutableList.of(
                    Text.of("Files modified (" + modifiedFiles.size() + "): "),
                    ListLayout.of(
                        modifiedFiles.stream()
                            .map(GenericEventRenderer::render)
                            .collect(toImmutableList()))));

        // 2. resolvedDependencies
        final Observable<ImmutableList<Component>> resolvedDependencies = events
            .ofType(ResolvedDependenciesEvent.class)
            .map(GenericEventRenderer::render)
            .startWith(StackLayout.of())
            .map(ImmutableList::of);

        // 3. notifications
        final Observable<ImmutableList<Component>> notifications = events
            .ofType(Notification.class)
            .map(GenericEventRenderer::render)
            .scan(ImmutableList.of(), MoreLists::append);

        return Observable.combineLatestDelayError(
            ImmutableList.of(modifiedSummary, resolvedDependencies, notifications),
            objects -> Arrays.stream(objects)
                .map(x -> (ImmutableList<Component>) x)
                .map(x -> (Component) StackLayout.of(x))
                .collect(ImmutableList.toImmutableList()))
            .map(StackLayout::of)
            .cast(Component.class);
    }
}
