package com.loopperfect.buckaroo.views;

import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.DependencyInstallationProgress;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.events.FileWriteEvent;
import com.loopperfect.buckaroo.events.ReadProjectFileEvent;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.tasks.DownloadProgress;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.*;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Created by gaetano on 17/06/17.
 */
public class ProgressView {

    private ProgressView(){}

    public static Observable<Component> progressView(final Observable<Event> events$) {

        final Observable<ReadProjectFileEvent> projectFiles$ = events$
            .ofType(ReadProjectFileEvent.class);

        final Observable<DependencyInstallationProgress> downloads$ = events$
            .ofType(DependencyInstallationProgress.class);

        final Observable<ResolvedDependenciesEvent> resolvedDependencies$ = events$
            .ofType(ResolvedDependenciesEvent.class);

        final Observable<FileWriteEvent> fileWrites$ = events$
            .ofType(FileWriteEvent.class);

        final Observable<Component> projects$ = projectFiles$
            .map(file -> Text.of(
                "reading project file " + file.project.name.orElse("") + " " +
                    file.project.license.orElse("")));

        final Observable<String> writes$  = fileWrites$
            .map(file -> file.path.toString());

        final Observable<ImmutableList<String>>  deps$ = resolvedDependencies$
            .map(deps->deps.dependencies)
            .map(deps->deps.dependencies.entrySet().stream())
            .map(s->s.map( kv ->
                kv.getKey().organization.toString()
                    + "/"
                    + kv.getKey().recipe.toString()
                    + "@" + kv.getValue().getValue0().toString()+" "))
            .map(s->s.collect(toImmutableList()));

        return Observable.combineLatest(
            projects$.startWith(FlowLayout.of()),
            writes$
                .map(w->FlowLayout.of(Text.of("modified: "), Text.of(w, Color.YELLOW)))
                .startWith(FlowLayout.of()),

            downloads$.map(
                (DependencyInstallationProgress d)-> {
                    final ImmutableList<Triplet<String, Long, Long>> downloading = d.progress
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue() instanceof DownloadProgress)
                        .map(e -> Pair.with(e.getKey(),(DownloadProgress)e.getValue()))
                        .map(e -> Triplet.with(
                            "downloading " + e.getValue0().identifier.toString(),
                            e.getValue1().downloaded,
                            e.getValue1().contentLength))
                        .collect(toImmutableList());

                    final long completed = downloading
                        .stream()
                        .map(Triplet::getValue1)
                        .reduce(1l, (a, b) -> a + b);

                    final long toDownload = downloading
                        .stream()
                        .map(Triplet::getValue2)
                        .reduce(1l, (a, b) -> a + b);

                    final Triplet<String, Long, Long> total = Triplet.with(
                        "total progress",
                        completed,
                        toDownload
                    );

                    final Comparator<Triplet<String, Long, Long>> comparator = Comparator.comparingDouble( a ->
                        1.0 - (double)a.getValue1() / (double)a.getValue2()
                    );

                    final ImmutableList<Triplet<String, Long, Long>> downloadView = downloading
                        .stream()
                        .filter(e-> e.getValue2() > e.getValue1())
                        .sorted(comparator)
                        .limit(5)
                        .collect(toImmutableList());

                    final ImmutableList<Triplet<String, Long, Long>> combined =
                        new ImmutableList.Builder<Triplet<String, Long, Long>>()
                            .addAll(downloadView)
                            .add(total)
                            .build();

                    return StackLayout.of(combined.stream()
                        .map(e-> StackLayout.of(
                            Text.of(e.getValue0()+" : "),
                            ProgressBar.of(
                                Math.min(
                                    (float)1,
                                    (float)e.getValue1() / Math.max((float)1,(float)e.getValue2())))))
                        .collect(toImmutableList()));
                }),


            deps$
                .map(Collection::stream)
                .map(s->FlowLayout.of(
                    s.map(x->Text.of(x, Color.GREEN)).collect(toImmutableList())))
                .startWith(FlowLayout.of()),

            StackLayout::of)
            .cast(Component.class)
            .sample(100, TimeUnit.MILLISECONDS, Schedulers.io());
    }
}
