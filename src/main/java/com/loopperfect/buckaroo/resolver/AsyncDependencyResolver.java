package com.loopperfect.buckaroo.resolver;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.Process;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.javatuples.Pair;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class AsyncDependencyResolver {

    private AsyncDependencyResolver() {}

    private static Process<Event, ResolvedDependencies> step(
        final RecipeSource recipeSource,
        final ResolvedDependencies resolved,
        final Dependency next,
        final ResolutionStrategy strategy) {

        Preconditions.checkNotNull(recipeSource);
        Preconditions.checkNotNull(resolved);
        Preconditions.checkNotNull(next);
        Preconditions.checkNotNull(strategy);

        if (resolved.dependencies.containsKey(next.project)) {
            final SemanticVersion resolvedVersion = resolved.dependencies.get(next.project).getValue0();
            return Process.of(
                Observable.just(ResolvedDependenciesEvent.of(resolved)),
                next.requirement.isSatisfiedBy(resolvedVersion) ?
                Single.just(resolved) :
                Single.error(new DependencyResolutionException(
                    next.project.encode() + "@" + resolvedVersion.encode() + " does not satisfy " + next.encode())));
        }

        return recipeSource.fetch(next.project)
            .chain(recipe -> {
                    final ImmutableList<Process<Event, ResolvedDependencies>> candidateStream = recipe.versions.entrySet()
                        .stream()
                        .filter(x -> next.requirement.isSatisfiedBy(x.getKey()))
                        .sorted(Comparator.comparing(Map.Entry::getKey))
                        .map(entry -> {

                            final ResolvedDependencies nextResolved = resolved.add(
                                next.project,
                                Pair.with(entry.getKey(), ResolvedDependency.from(entry.getValue())));

                            final ImmutableList<Dependency> nextDependencies = new ImmutableList.Builder<Dependency>()
                                .addAll(entry.getValue().dependencies.orElse(DependencyGroup.of()).entries())
                                .build();

                            return resolve(
                                recipeSource,
                                nextResolved,
                                nextDependencies,
                                strategy);
                        }).collect(toImmutableList());

                final Observable<Event> states =
                    Observable.merge(candidateStream
                        .stream()
                        .map(Process::states)
                        .collect(toImmutableList()));

                return Process.of(states, MoreObservables.findMax(
                    MoreObservables.skipErrors(Observable.fromIterable(
                        candidateStream.stream().map(Process::result)::iterator)),
                        Comparator.comparing(strategy::score))
                        .toSingle()
                        .onErrorResumeNext(error ->
                            Single.error(new DependencyResolutionException("Could not satisfy " + next, error))));
            });
    }

    private static Process<Event, ResolvedDependencies> resolve(
        final RecipeSource recipeSource,
        final ResolvedDependencies resolved,
        final ImmutableList<Dependency> dependencies,
        final ResolutionStrategy strategy) {

        Preconditions.checkNotNull(recipeSource);
        Preconditions.checkNotNull(resolved);
        Preconditions.checkNotNull(dependencies);
        Preconditions.checkNotNull(strategy);

        return Process.chainN(
            Process.just(resolved),
            dependencies.stream()
                .map((Dependency dependency) ->
                    (Function<ResolvedDependencies, Process<Event, ResolvedDependencies>>) x ->
                        step(recipeSource, x, dependency, strategy))
                .collect(ImmutableList.toImmutableList()));
    }

    public static <t, u> Process<t, u> process(final Process<t, u> f) {
        return f;
    }

    public static Process<Event, ResolvedDependencies> resolve(
        final RecipeSource recipeSource,
        final ImmutableList<Dependency> dependencies) {
        return resolve(recipeSource, ResolvedDependencies.of(), dependencies, SumResolutionStrategy.of());
    }
}
