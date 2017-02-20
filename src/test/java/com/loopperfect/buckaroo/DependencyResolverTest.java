package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by gaetano on 16/02/17.
 */
public class DependencyResolverTest {

    @Test
    public void getLatestOfNone() throws Exception {
        ImmutableMap<SemanticVersion, Project> versions = ImmutableMap.of();
        assertFalse(DependencyResolver.getLatest(versions).isPresent());
    }


    @Test
    public void getLatestOfThree() throws Exception {
        ImmutableMap<SemanticVersion, Project> versions = ImmutableMap.of(
            SemanticVersion.of(2), Project.of( Identifier.of("foo2"), Optional.empty(), ImmutableMap.of()),
            SemanticVersion.of(3), Project.of( Identifier.of("foo3"), Optional.empty(), ImmutableMap.of()),
            SemanticVersion.of(1), Project.of( Identifier.of("foo1"), Optional.empty(), ImmutableMap.of())
        );

        assertTrue(
            SemanticVersion.of(3).equals( DependencyResolver.getLatest(versions).get().getKey() )
        );
    }

    private static DependencyFetcher createFetcher(ImmutableMap<Identifier, ImmutableMap<SemanticVersion,Project>> projects) {
        DependencyFetcher fetcher = (id, requirement) -> {

            if(!projects.containsKey(id)) {
                return Either.left(
                    new ProjectNotFoundException(id)
                );
            }

            final Map<SemanticVersion, Project> candidates =
                projects.getOrDefault(id, ImmutableMap.of())
                    .entrySet()
                    .stream()
                    .filter(entry -> requirement.isSatisfiedBy(entry.getKey()))
                    .collect(Collectors.toMap(k->k.getKey(),v->v.getValue()));

            if(candidates.isEmpty())
                return Either.left(
                    new VersionRequirementNotSatisfiedException(id, requirement)
                );

            return Either.right(
                ImmutableMap.copyOf(candidates)
            );
        };

        return fetcher;
    }


    @Test
    public void resolveSimple() throws Exception {

        Project project = Project.of( Identifier.of("project"), Optional.empty(), ImmutableMap.of(
            Identifier.of("foo"), ExactSemanticVersion.of(SemanticVersion.of(1)),
            Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1))
        ));

        Project foo = Project.of( Identifier.of("foo"), Optional.empty(), ImmutableMap.of());
        Project bar = Project.of( Identifier.of("bar"), Optional.empty(), ImmutableMap.of(
            Identifier.of("baz"), ExactSemanticVersion.of(SemanticVersion.of(1))
        ));

        Project baz = Project.of( Identifier.of("baz"), Optional.empty(), ImmutableMap.of());

        ImmutableMap<Identifier, ImmutableMap<SemanticVersion, Project>> projects =
            ImmutableMap.of(
                foo.name, ImmutableMap.of( SemanticVersion.of(1), foo ),
                bar.name, ImmutableMap.of( SemanticVersion.of(1), bar ),
                baz.name, ImmutableMap.of( SemanticVersion.of(1), baz ));


        DependencyFetcher fetcher = createFetcher(projects);

        ImmutableMap<Project, SemanticVersion> toInstall = DependencyResolver.resolve(project, fetcher).toOptional().get();

        assertEquals(ImmutableMap.of(
            foo, SemanticVersion.of(1),
            bar, SemanticVersion.of(1),
            baz, SemanticVersion.of(1)
        ), toInstall);
    }

    @Test
    public void resolveCircular() throws Exception {

        Project project = Project.of( Identifier.of("project"), Optional.empty(), ImmutableMap.of(
            Identifier.of("foo"), ExactSemanticVersion.of(SemanticVersion.of(1)),
            Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1))
        ));

        Project foo = Project.of( Identifier.of("foo"), Optional.empty(), ImmutableMap.of());
        Project bar = Project.of( Identifier.of("bar"), Optional.empty(), ImmutableMap.of(
            Identifier.of("baz"), ExactSemanticVersion.of(SemanticVersion.of(1))
        ));

        Project baz = Project.of( Identifier.of("baz"), Optional.empty(), ImmutableMap.of(
            Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1))
        ));

        ImmutableMap<Identifier, ImmutableMap<SemanticVersion, Project>> projects =
            ImmutableMap.of(
                foo.name, ImmutableMap.of( SemanticVersion.of(1), foo ),
                bar.name, ImmutableMap.of( SemanticVersion.of(1), bar ),
                baz.name, ImmutableMap.of( SemanticVersion.of(1), baz ));

        DependencyFetcher fetcher = createFetcher(projects);

        ImmutableMap<Project, SemanticVersion> toInstall = DependencyResolver.resolve(project, fetcher).toOptional().get();

        assertEquals(ImmutableMap.of(
            foo, SemanticVersion.of(1),
            bar, SemanticVersion.of(1),
            baz, SemanticVersion.of(1)
        ), toInstall);
    }


    @Test
    public void resolveFailure() throws Exception {

        Project project = Project.of(Identifier.of("project"), Optional.empty(), ImmutableMap.of(
            Identifier.of("foo"), ExactSemanticVersion.of(SemanticVersion.of(2)),
            Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1))
        ));

        Project foo = Project.of(Identifier.of("foo"), Optional.empty(), ImmutableMap.of());
        Project bar = Project.of(Identifier.of("bar"), Optional.empty(), ImmutableMap.of(
            Identifier.of("baz"), ExactSemanticVersion.of(SemanticVersion.of(1))
        ));

        Project baz = Project.of(Identifier.of("baz"), Optional.empty(), ImmutableMap.of(
            Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1))
        ));

        ImmutableMap<Identifier, ImmutableMap<SemanticVersion, Project>> projects =
            ImmutableMap.of(
                foo.name, ImmutableMap.of(SemanticVersion.of(1), foo),
                bar.name, ImmutableMap.of(SemanticVersion.of(1), bar),
                baz.name, ImmutableMap.of(SemanticVersion.of(1), baz));

        DependencyFetcher fetcher = createFetcher(projects);


        List<DependencyResolverException> unresolved = DependencyResolver.resolve(project, fetcher)
            .join(e -> e, x -> null);

        assertEquals(ImmutableList.of(
            new VersionRequirementNotSatisfiedException(
                Identifier.of("foo"),
                ExactSemanticVersion.of(SemanticVersion.of(2)))
        ), unresolved);
    }
}