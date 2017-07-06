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
import com.loopperfect.buckaroo.virtualterminal.components.*;
import io.reactivex.Observable;

import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class SummaryView {

    private SummaryView() {

    }

    public static Observable<Component> render(final Observable<Event> events) {

        Preconditions.checkNotNull(events);

        final Observable<Event> fileModifiedEvent = events
            .filter(x -> x instanceof WriteFileEvent || x instanceof TouchFileEvent);

        final Observable<ImmutableList<Component>> modifiedSummary = fileModifiedEvent.scan(
            ImmutableList.of(),
            (ImmutableList<Event> list, Event event) -> Streams
                .concat(list.stream(), Stream.of(event))
                .collect(toImmutableList()))
            .map((ImmutableList<Event> modifiedFiles) -> modifiedFiles.isEmpty() ?
                ImmutableList.of(Text.of("No files were modified. ")) :
                ImmutableList.of(
                        Text.of("Files modified (" + modifiedFiles.size() + "): "),
                            ListLayout.of(
                                modifiedFiles.stream()
                                    .map(GenericEventRenderer::render)
                                    .collect(toImmutableList()))));

        final Observable<ImmutableList<Component>> resolvedDependencies = events
            .ofType(ResolvedDependenciesEvent.class)
            .map(GenericEventRenderer::render)
            .map(CachedComponent::of)
            .startWith(StackLayout.of())
            .map(CachedComponent::of)
            .map(ImmutableList::of);

        final Observable<ImmutableList<Component>> notifications = events
            .ofType(Notification.class)
            .map(GenericEventRenderer::render)
            .map(CachedComponent::of)
            .scan(ImmutableList.of(), MoreLists::append);

        return Observable.combineLatest(
            modifiedSummary, resolvedDependencies, notifications, MoreLists::concat)
            .map(StackLayout::of)
            .map(CachedComponent::of)
            .cast(Component.class);
    }
}
