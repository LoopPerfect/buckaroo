package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.versioning.BoundedSemanticVersion;
import com.loopperfect.buckaroo.versioning.ExactSemanticVersion;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CLIParsersTest {

    @Test
    public void testIdentifierParser() {

        final Parser<Identifier> parser = CLIParsers.identifierParser;

        assertEquals(Identifier.of("xz"), parser.parse("xz"));
        assertEquals(Identifier.of("abc"), parser.parse("abc"));
        assertEquals(Identifier.of("a123-12"), parser.parse("a123-12"));
        assertEquals(Identifier.of("abc"), parser.parse("abc"));
        assertEquals(Identifier.of("abcde_fgh"), parser.parse("abcde_fgh"));
        assertEquals(Identifier.of("abcde_-"), parser.parse("abcde_-"));
        assertEquals(Identifier.of("000abc"), parser.parse("000abc"));

        final String a30 = String.join("", Collections.nCopies(30, "a"));
        assertEquals(Identifier.of(a30), parser.parse(a30));

        try {
            parser.parse("++abcde_-+++");
            assertTrue(false);
        } catch (final ParserException e) {
            assertTrue(true);
        }

        try {
            parser.parse("a");
            assertTrue(false);
        } catch (final ParserException e) {
            assertTrue(true);
        }

        try {
            parser.parse(String.join("", Collections.nCopies(31, "a")));
            assertTrue(false);
        } catch (final ParserException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testRecipeIdentifierParser() {

        final Parser<RecipeIdentifier> parser = CLIParsers.recipeIdentifierParser;

        assertEquals(RecipeIdentifier.of("boost", "any"), parser.parse("boost/any"));
    }

    @Test
    public void testPartialRecipeIdentifierParser() {

        final Parser<PartialRecipeIdentifier> parser = CLIParsers.partialRecipeIdentifierParser;

        assertEquals(
            PartialRecipeIdentifier.of(Identifier.of("org"), Identifier.of("awesome")),
            parser.parse("org/awesome"));

        assertEquals(
            PartialRecipeIdentifier.of(Identifier.of("loopperfect"), Identifier.of("valuable")),
            parser.parse("  loopperfect/valuable "));

        assertEquals(
            PartialRecipeIdentifier.of(
                Identifier.of("loopperfect"),
                Identifier.of("valuable")),
            parser.parse(" loopperfect /  valuable       "));

        assertEquals(
            PartialRecipeIdentifier.of(
                Identifier.of("github"),
                Identifier.of("loopperfect"),
                Identifier.of("neither")),
            parser.parse(" github  + loopperfect /  neither       "));

        assertEquals(
            PartialRecipeIdentifier.of(
                Identifier.of("github"),
                Identifier.of("loopperfect"),
                Identifier.of("neither")),
            parser.parse("github+loopperfect/neither       "));
    }

    @Test
    public void testPartialDependencyParser() {

        final Parser<PartialDependency> parser = CLIParsers.partialDependencyParser;

        assertEquals(
            PartialDependency.of(Identifier.of("org"), Identifier.of("awesome")),
            parser.parse("org/awesome"));

        assertEquals(
            PartialDependency.of(Identifier.of("loopperfect"), Identifier.of("valuable")),
            parser.parse("  loopperfect/valuable "));

        assertEquals(
            PartialDependency.of(
                Optional.empty(),
                Identifier.of("loopperfect"),
                Identifier.of("valuable"),
                Optional.of(ExactSemanticVersion.of(SemanticVersion.of(1)))),
            parser.parse(" loopperfect /  valuable       1"));

        assertEquals(
            PartialDependency.of(
                Optional.of(Identifier.of("github")),
                Identifier.of("loopperfect"),
                Identifier.of("neither"),
                Optional.of(ExactSemanticVersion.of(SemanticVersion.of(1)))),
            parser.parse("github  + loopperfect /  neither       [ 1 ]"));
    }

    @Test
    public void testCommandParser() {

        final Parser<CLICommand> parser = CLIParsers.commandParser;

        assertEquals(InitCommand.of(), parser.parse(" init  "));

        assertEquals(UpgradeCommand.of(), parser.parse("upgrade"));

        assertEquals(InstallExistingCommand.of(), parser.parse(" install "));

        assertEquals(VersionCommand.of(), parser.parse("   version "));

        assertEquals(HelpCommand.of(), parser.parse("help "));

        assertEquals(
            InstallCommand.of(PartialDependency.of(Identifier.of("org"), Identifier.of("awesome"))),
            CLIParsers.commandParser.parse(" install   org/awesome  "));

        assertEquals(
            InstallCommand.of(PartialDependency.of(Identifier.of("3hren"), Identifier.of("blackhole"))),
            CLIParsers.commandParser.parse(" install   3hren/blackhole  "));

        assertEquals(
            InstallCommand.of(PartialDependency.of(
                Identifier.of("org"), Identifier.of("some_lib"), BoundedSemanticVersion.atLeast(SemanticVersion.of(2)))),
            CLIParsers.commandParser.parse(" install  org/some_lib>=2  "));

        assertEquals(
            InstallCommand.of(PartialDependency.of(
                Identifier.of("org"), Identifier.of("another-lib2"), ExactSemanticVersion.of(SemanticVersion.of(2)))),
            CLIParsers.commandParser.parse("install org/another-lib2 [ 2 ]  "));

        assertEquals(
            InstallCommand.of(
                PartialDependency.of(Identifier.of("org"), Identifier.of("aaa")),
                PartialDependency.of(Identifier.of("org"), Identifier.of("bbb"))),
            CLIParsers.commandParser.parse(" install   org/aaa  org/bbb  "));

        assertEquals(
            UninstallCommand.of(RecipeIdentifier.of("org", "some_lib")),
            CLIParsers.commandParser.parse(" uninstall   org/some_lib "));


        assertEquals(
            UninstallCommand.of(PartialRecipeIdentifier.of(
                Identifier.of("github"), Identifier.of("org"), Identifier.of("some_lib"))),
            CLIParsers.commandParser.parse(" uninstall   github+org/some_lib "));

        assertEquals(
            UpdateCommand.of(),
            CLIParsers.commandParser.parse("     update      "));

        assertEquals(
                QuickstartCommand.of(),
                CLIParsers.commandParser.parse("quickstart"));

        assertEquals(
            ResolveCommand.of(),
            CLIParsers.commandParser.parse("     resolve "));

        try {
            parser.parse("installsomething");
            assertTrue(false);
        } catch (final ParserException e) {
            assertTrue(true);
        }
    }
}