package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by gaetano on 16/02/17.
 */
public class DependencyResolverTest {

    @Test
    public void resolveSimple() throws Exception {

        final Project project = Project.of("project", DependencyGroup.of(ImmutableMap.of(
            Identifier.of("foo"), ExactSemanticVersion.of(SemanticVersion.of(1)),
            Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1)))));

        final DependencyFetcher fetcher = DependencyFetcherFromMap.of(
                ImmutableMap.of(
                        Identifier.of("foo"), ImmutableMap.of(SemanticVersion.of(1), DependencyGroup.of()),
                        Identifier.of("bar"), ImmutableMap.of(
                                SemanticVersion.of(1),
                                DependencyGroup.of(ImmutableMap.of(
                                        Identifier.of("baz"), ExactSemanticVersion.of(SemanticVersion.of(1))))),
                        Identifier.of("baz"), ImmutableMap.of(
                                SemanticVersion.of(1), DependencyGroup.of())));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>> expected =
                Either.right(ImmutableMap.of(
                        Identifier.of("foo"), SemanticVersion.of(1),
                        Identifier.of("bar"), SemanticVersion.of(1),
                        Identifier.of("baz"), SemanticVersion.of(1)));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>> actual =
                DependencyResolver.resolve(project.dependencies, fetcher);

        assertEquals(expected, actual);
    }

    @Test
    public void resolveCircular() throws Exception {

        final Project project = Project.of("project", DependencyGroup.of(ImmutableMap.of(
                Identifier.of("foo"), ExactSemanticVersion.of(SemanticVersion.of(1)),
                Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1)))));

        final DependencyFetcher fetcher = DependencyFetcherFromMap.of(ImmutableMap.of(
                Identifier.of("foo"), ImmutableMap.of(SemanticVersion.of(1), DependencyGroup.of()),
                Identifier.of("bar"), ImmutableMap.of(
                        SemanticVersion.of(1), DependencyGroup.of(ImmutableMap.of(
                                Identifier.of("baz"), ExactSemanticVersion.of(SemanticVersion.of(1))))),
                Identifier.of("baz"), ImmutableMap.of(
                        SemanticVersion.of(1), DependencyGroup.of(ImmutableMap.of(
                                Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1)))))));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>> expected =
                Either.right(ImmutableMap.of(
                        Identifier.of("foo"), SemanticVersion.of(1),
                        Identifier.of("bar"), SemanticVersion.of(1),
                        Identifier.of("baz"), SemanticVersion.of(1)));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>> actual =
                DependencyResolver.resolve(project.dependencies, fetcher);

        assertEquals(expected, actual);
    }


    @Test
    public void resolveFailure() throws Exception {

        final Project project = Project.of("project", DependencyGroup.of(ImmutableMap.of(
                Identifier.of("foo"), ExactSemanticVersion.of(SemanticVersion.of(2)),
                Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1)))));

        final DependencyFetcher fetcher = DependencyFetcherFromMap.of(ImmutableMap.of(
                Identifier.of("foo"), ImmutableMap.of(SemanticVersion.of(1), DependencyGroup.of()),
                Identifier.of("bar"), ImmutableMap.of(SemanticVersion.of(1), DependencyGroup.of(ImmutableMap.of(
                        Identifier.of("baz"), ExactSemanticVersion.of(SemanticVersion.of(1))))),
                Identifier.of("baz"), ImmutableMap.of(SemanticVersion.of(1), DependencyGroup.of(ImmutableMap.of(
                        Identifier.of("bar"), ExactSemanticVersion.of(SemanticVersion.of(1)))))));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>> expected =
                Either.left(ImmutableList.of(
                        new VersionRequirementNotSatisfiedException(
                                Identifier.of("foo"),
                                ExactSemanticVersion.of(SemanticVersion.of(2)))));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<Identifier, SemanticVersion>> actual =
                DependencyResolver.resolve(project.dependencies, fetcher);

        assertEquals(expected, actual);
    }

    @Test
    public void resolveEmpty() throws Exception {

        final DependencyFetcher fetcher = DependencyFetcherFromMap.of(ImmutableMap.of());

        assertEquals(
                Either.right(ImmutableMap.of()),
                DependencyResolver.resolve(DependencyGroup.of(), fetcher));
    }

    @Test
    public void resolveAll() throws Exception {

        final Identifier a = Identifier.of("alpha");
        final Identifier b = Identifier.of("bravo");
        final Identifier c = Identifier.of("charlie");

        final DependencyGroup dependencyGroup = DependencyGroup.of(ImmutableMap.of(
                a, AnySemanticVersion.of()));

        final DependencyFetcher fetcher = DependencyFetcherFromMap.of(ImmutableMap.of(
                a, ImmutableMap.of(SemanticVersion.of(1), DependencyGroup.of()),
                b, ImmutableMap.of(SemanticVersion.of(1), DependencyGroup.of(
                        ImmutableMap.of(c, AnySemanticVersion.of()))),
                c, ImmutableMap.of(SemanticVersion.of(1), DependencyGroup.of())));

        assertEquals(
                Either.right(ImmutableMap.of(a, SemanticVersion.of(1))),
                DependencyResolver.resolve(dependencyGroup, fetcher));
    }
}