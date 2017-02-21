package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by gaetano on 13/02/17.
 */


public final class DependencyResolver {

    private DependencyResolver() {

    }

    public static Optional<ImmutableMap.Entry<SemanticVersion, Project>> getLatest(
            ImmutableMap<SemanticVersion, Project> versions) {
      return versions
              .entrySet()
              .stream()
              .max(Comparator.comparing(Map.Entry::getKey));
    }

    public static Either<
        List<DependencyResolverException>,
        ImmutableMap<Project, SemanticVersion>> resolve(Project p, DependencyFetcher proj) {

        Stack< Project > todo = new Stack<>();
        Set< Identifier > seen = new HashSet<>();
        HashMap< Project, SemanticVersion > deps = new HashMap<>(); //TODO: extend to Set<SemanticVersion> and narrow down using Requirements...
        List<DependencyResolverException> unresolved = new ArrayList<>();
        todo.push(p);
        seen.add(p.name);

        while (!todo.isEmpty()) {
            final Project toResolve = todo.pop();
            final List<
                Either<
                    DependencyResolverException,
                    ImmutableMap.Entry<SemanticVersion, Project>>>  next =
                    toResolve.dependencies
                    .entrySet()
                    .stream()
                    .filter(x->!seen.contains(x.getKey()))
                    .map(d -> proj.fetch(d.getKey(), d.getValue()))
                    .map(x -> x.rightProjection(y->getLatest(y).get()))
                    //.flatMap(x->x.map(i->Stream.of(i)).orElse(Stream.empty()))
                    .collect(Collectors.toList());

            for (Either<DependencyResolverException, ImmutableMap.Entry<SemanticVersion, Project>> item : next) {

                Optional<ImmutableMap.Entry<SemanticVersion,Project>> resolvedProject = item.join(
                    x->Optional.empty(),
                    x->Optional.of(x)
                );

                Optional<DependencyResolverException> unresolvedProject = item.join(
                    x->Optional.of(x),
                    x->Optional.empty()
                );

                if (unresolvedProject.isPresent()) {
                    DependencyResolverException failed = unresolvedProject.get();
                    unresolved.add(failed);
                    seen.add(failed.getIdentifier());
                }

                if (resolvedProject.isPresent()) {
                    ImmutableMap.Entry<SemanticVersion, Project> successful = resolvedProject.get();
                    seen.add(successful.getValue().name);
                    todo.add(successful.getValue());
                    deps.put(successful.getValue(), successful.getKey());
                }
            }
        }

        if (!unresolved.isEmpty()) {
            return Either.left(
                ImmutableList.copyOf(unresolved)
            );
        }

        return Either.right(
            ImmutableMap.copyOf(deps)
        );
    }
}
