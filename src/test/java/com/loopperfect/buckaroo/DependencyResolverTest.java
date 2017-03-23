package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class DependencyResolverTest {

    @Test
    public void resolveSimple() throws Exception {

        final Project project = Project.of("project", DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "foo"), ExactSemanticVersion.of(SemanticVersion.of(1)),
                RecipeIdentifier.of("org", "bar"), ExactSemanticVersion.of(SemanticVersion.of(1)))));

        final CookBook cookBook = CookBook.of(ImmutableMap.of(Identifier.of("org"),
                Organization.of("Org", ImmutableMap.of(
                        Identifier.of("foo"),
                        Recipe.of(
                                "Foo",
                                "foo.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.bar.com/bar.git", "hadvrddg")))),
                        Identifier.of("bar"),
                        Recipe.of(
                                "Bar",
                                "bar.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.bar.com/bar.git", "hadvrddg"),
                                                DependencyGroup.of(ImmutableMap.of(
                                                        RecipeIdentifier.of("org", "baz"), ExactSemanticVersion.of(SemanticVersion.of(1))))))),
                        Identifier.of("baz"),
                        Recipe.of(
                                "Baz",
                                "baz.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.baz.com/baz.git", "hadvrddg"))))))));

        final DependencyFetcher fetcher = CookbookDependencyFetcher.of(cookBook);

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<RecipeIdentifier, SemanticVersion>> expected =
            Either.right(ImmutableMap.of(
                RecipeIdentifier.of("org", "foo"), SemanticVersion.of(1),
                RecipeIdentifier.of("org", "bar"), SemanticVersion.of(1),
                RecipeIdentifier.of("org", "baz"), SemanticVersion.of(1)));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<RecipeIdentifier, SemanticVersion>> actual =
            DependencyResolver.resolve(project.dependencies, fetcher);

        assertEquals(expected, actual);
    }

    @Test
    public void resolveCircular() throws Exception {

        final Project project = Project.of("project", DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "foo"), ExactSemanticVersion.of(SemanticVersion.of(1)),
                RecipeIdentifier.of("org", "bar"), ExactSemanticVersion.of(SemanticVersion.of(1)))));

        final CookBook cookBook = CookBook.of(ImmutableMap.of(Identifier.of("org"),
                Organization.of("Org", ImmutableMap.of(
                        Identifier.of("foo"),
                        Recipe.of(
                                "Foo",
                                "foo.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(GitCommit.of("git@git.foo.com/foo.git", "hadvrddg")))),
                        Identifier.of("bar"),
                        Recipe.of(
                                "Bar",
                                "bar.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.bar.com/bar.git", "hadvrddg"),
                                                DependencyGroup.of(ImmutableMap.of(
                                                        RecipeIdentifier.of("org", "baz"),
                                                        ExactSemanticVersion.of(SemanticVersion.of(1))))))),
                        Identifier.of("baz"),
                        Recipe.of(
                                "Baz",
                                "baz.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.baz.com/baz.git", "hadvrddg"),
                                                DependencyGroup.of(ImmutableMap.of(
                                                        RecipeIdentifier.of("org", "bar"),
                                                        ExactSemanticVersion.of(SemanticVersion.of(1)))))))))));

        final DependencyFetcher fetcher = CookbookDependencyFetcher.of(cookBook);

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<RecipeIdentifier, SemanticVersion>> expected =
            Either.right(ImmutableMap.of(
                    RecipeIdentifier.of("org", "foo"), SemanticVersion.of(1),
                    RecipeIdentifier.of("org", "bar"), SemanticVersion.of(1),
                    RecipeIdentifier.of("org", "baz"), SemanticVersion.of(1)));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<RecipeIdentifier, SemanticVersion>> actual =
            DependencyResolver.resolve(project.dependencies, fetcher);

        assertEquals(expected, actual);
    }

    @Test
    public void resolveFailure() throws Exception {

        final Project project = Project.of("project", DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "foo"), ExactSemanticVersion.of(SemanticVersion.of(2)),
                RecipeIdentifier.of("org", "bar"), ExactSemanticVersion.of(SemanticVersion.of(1)))));

        final CookBook cookBook = CookBook.of(ImmutableMap.of(
                Identifier.of("org"), Organization.of("Org", ImmutableMap.of(
                        Identifier.of("foo"),
                        Recipe.of(
                                "Foo",
                                "foo.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.foo.com/foo.git", "hadvrddg")))),
                        Identifier.of("bar"),
                        Recipe.of(
                                "Bar",
                                "bar.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.bar.com/bar.git", "hadvrddg"),
                                                DependencyGroup.of(ImmutableMap.of(
                                                        RecipeIdentifier.of("org", "baz"),
                                                        ExactSemanticVersion.of(SemanticVersion.of(1))))))),
                        Identifier.of("baz"),
                        Recipe.of(
                                "Baz",
                                "baz.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.baz.com/baz.git", "hadvrddg"),
                                                DependencyGroup.of(ImmutableMap.of(
                                                        RecipeIdentifier.of("org", "bar"),
                                                        ExactSemanticVersion.of(SemanticVersion.of(1)))))))))));

        final DependencyFetcher fetcher = CookbookDependencyFetcher.of(cookBook);

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<RecipeIdentifier, SemanticVersion>> expected =
            Either.left(ImmutableList.of(
                new VersionRequirementNotSatisfiedException(
                        RecipeIdentifier.of("org", "foo"),
                        ExactSemanticVersion.of(SemanticVersion.of(2)))));

        final Either<ImmutableList<DependencyResolverException>, ImmutableMap<RecipeIdentifier, SemanticVersion>> actual =
            DependencyResolver.resolve(project.dependencies, fetcher);

        assertEquals(expected, actual);
    }

    @Test
    public void resolveEmpty() throws Exception {

        final DependencyFetcher fetcher = CookbookDependencyFetcher.of(ImmutableList.of());

        assertEquals(
                Either.right(ImmutableMap.of()),
                DependencyResolver.resolve(DependencyGroup.of(), fetcher));
    }

    @Test
    public void resolveAll() throws Exception {

        final RecipeIdentifier a = RecipeIdentifier.of(Identifier.of("org"), Identifier.of("alpha"));
        final RecipeIdentifier b = RecipeIdentifier.of(Identifier.of("org"), Identifier.of("bravo"));
        final RecipeIdentifier c = RecipeIdentifier.of(Identifier.of("org"), Identifier.of("charlie"));

        final DependencyGroup dependencyGroup = DependencyGroup.of(ImmutableMap.of(
                a, AnySemanticVersion.of()));

        final CookBook cookBook = CookBook.of(ImmutableMap.of(Identifier.of("org"), Organization.of(
                "Org", ImmutableMap.of(
                        Identifier.of("alpha"),
                        Recipe.of(
                                "Alpha",
                                "alpha.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.alpha.com/alpha.git", "hadvrddg")))),
                        Identifier.of("bravo"),
                        Recipe.of(
                                "Bravo",
                                "bravo.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.bravo.com/bravo.git", "hadvrddg"),
                                                DependencyGroup.of(ImmutableMap.of(c, AnySemanticVersion.of()))))),
                        Identifier.of("charlie"),
                        Recipe.of(
                                "Charlie",
                                "charlie.com",
                                ImmutableMap.of(
                                        SemanticVersion.of(1),
                                        RecipeVersion.of(
                                                GitCommit.of("git@git.charlie.com/charlie.git", "hadvrddg"),
                                                DependencyGroup.of())))))));

        final DependencyFetcher fetcher = CookbookDependencyFetcher.of(cookBook);

        assertEquals(
                Either.right(ImmutableMap.of(a, SemanticVersion.of(1))),
                DependencyResolver.resolve(dependencyGroup, fetcher));
    }
}