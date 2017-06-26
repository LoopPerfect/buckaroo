package com.loopperfect.buckaroo.views;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.MoreLists;
import com.loopperfect.buckaroo.events.WriteFileEvent;
import com.loopperfect.buckaroo.events.TouchFileEvent;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.FlowLayout;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;
import io.reactivex.Observable;

import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class SummaryView {

    private SummaryView() {

    }

    public static Observable<Component> summaryView(final Observable<Event> events) {

        Preconditions.checkNotNull(events);

        final Observable<Event> fileModifiedEvent = events
            .filter(x -> x instanceof WriteFileEvent || x instanceof TouchFileEvent);

        final Observable<ImmutableList<Component>> modifiedSummary = fileModifiedEvent.scan(
            ImmutableList.of(),
            (ImmutableList<Event> list, Event event) -> Streams
                .concat(list.stream(), Stream.of(event))
                .collect(toImmutableList()))
            .map((ImmutableList<Event> modifiedFiles) -> ImmutableList.of(
                Text.of("Files modified (" + modifiedFiles.size() + "): "),
                FlowLayout.of(
                    Text.of("  "),
                    StackLayout.of(
                        modifiedFiles.stream()
                            .map(EventRenderer::render)
                            .collect(toImmutableList())))));

        final Observable<ImmutableList<Component>> resolvedDependencies = events
            .ofType(ResolvedDependenciesEvent.class)
            .map(EventRenderer::render)
            .map(ImmutableList::of);

        return Observable.combineLatest(modifiedSummary, resolvedDependencies, MoreLists::concat)
            .map(StackLayout::of)
            .cast(Component.class);
    }
}
