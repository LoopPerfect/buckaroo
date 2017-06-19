package com.loopperfect.buckaroo.views;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.events.FileWriteEvent;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.FlowLayout;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;
import io.reactivex.Observable;

import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Created by gaetano on 17/06/17.
 */
public class SummaryView {

    public static Observable<Component> summaryView(Observable<Event> events$) {

        final Observable<FileWriteEvent> fileWrites$ = events$
            .ofType(FileWriteEvent.class);

        final Observable<ResolvedDependenciesEvent> resolvedDependencies$ = events$
            .ofType(ResolvedDependenciesEvent.class);

        final Observable<ImmutableList<String>> deps$ = resolvedDependencies$
            .map(deps -> deps.dependencies)
            .map(deps -> deps.dependencies.entrySet().stream())
            .map(s -> s.map(kv ->
                kv.getKey().organization.toString()
                    + "/"
                    + kv.getKey().recipe.toString()
                    + "@" + kv.getValue().getValue0().toString()))
            .map(s -> s.collect(toImmutableList()));

        final Observable<String> writes$ = fileWrites$
            .map(file -> file.path.toString());


        final Observable<ImmutableList<Component>> modifiedSummary = writes$.scan(
            ImmutableList.of(),
            (ImmutableList<String> list, String file) -> Streams
                .concat(list.stream(), Stream.of(file))
                .collect(toImmutableList()))
            .map((ImmutableList<String> modifiedFiles) -> ImmutableList.of(
                Text.of("modified Files(" + modifiedFiles.size() + ") :"),
                FlowLayout.of(
                    Text.of("    "),
                    StackLayout.of(
                        modifiedFiles.stream().map(Text::of).collect(toImmutableList())))));


        final Observable<ImmutableList<Component>> depsSummary = deps$
            .lastElement()
            .toObservable()
            .map(deps -> ImmutableList.of(
                Text.of("resolved dependencies(" + deps.size() + ") :"),
                FlowLayout.of(
                    Text.of("    "),
                    StackLayout.of(deps.stream().map(Text::of).collect(toImmutableList())))))
            .startWith(ImmutableList.of(FlowLayout.of()));


        return Observable
            .combineLatest(
                modifiedSummary,
                depsSummary,
                (m, d) -> new ImmutableList.Builder<Component>()
                    .addAll(m)
                    .addAll(d)
                    .build())
            .map(StackLayout::of)
            .cast(Component.class);
    }
}
