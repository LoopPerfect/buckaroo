package com.loopperfect.buckaroo.cli;

import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import java.util.Collections;

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
        assertEquals(Identifier.of("abcde_-+++"), parser.parse("abcde_-+++"));
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
    public void testCommandParser() {

        final Parser<CLICommand> parser = CLIParsers.commandParser;

        assertEquals(InitCommand.of(), parser.parse(" init  "));

        assertEquals(RecipesCommand.of(), parser.parse(" organizations  "));

        assertEquals(UpgradeCommand.of(), parser.parse("upgrade"));

        assertEquals(InstallExistingCommand.of(), parser.parse(" install "));

        assertEquals(GenerateCommand.of(), parser.parse("   generate "));

        assertEquals(CookbooksCommand.of(), parser.parse("   cookbooks "));

        assertEquals(VersionCommand.of(), parser.parse("   version "));

        assertEquals(HelpCommand.of(), parser.parse("help "));

        assertEquals(
            InstallCommand.of(RecipeIdentifier.of("org", "awesome")),
            CLIParsers.commandParser.parse(" install   org/awesome  "));

        assertEquals(
            InstallCommand.of(RecipeIdentifier.of("3hren", "blackhole")),
            CLIParsers.commandParser.parse(" install   3hren/blackhole  "));

        assertEquals(
            UninstallCommand.of(RecipeIdentifier.of("org", "some_lib")),
            CLIParsers.commandParser.parse(" uninstall   org/some_lib "));

        assertEquals(
            InstallCommand.of(RecipeIdentifier.of("org", "some_lib"), BoundedSemanticVersion.atLeast(SemanticVersion.of(2))),
            CLIParsers.commandParser.parse(" install  org/some_lib>=2  "));

        assertEquals(
            InstallCommand.of(RecipeIdentifier.of("org", "another-lib2"), ExactSemanticVersion.of(SemanticVersion.of(2))),
            CLIParsers.commandParser.parse("install org/another-lib2 [ 2 ]  "));

        assertEquals(
            UpdateCommand.of(Identifier.of("boost-config")),
            CLIParsers.commandParser.parse(" uPdAte  boost-config "));

        assertEquals(
            UpdateCommand.of(),
            CLIParsers.commandParser.parse("update"));

        assertEquals(
                QuickstartCommand.of(),
                CLIParsers.commandParser.parse("quickstart"));

        try {
            parser.parse("installsomething");
            assertTrue(false);
        } catch (final ParserException e) {
            assertTrue(true);
        }
    }
}