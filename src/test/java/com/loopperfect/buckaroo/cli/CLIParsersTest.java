package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.*;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;
import org.junit.Test;

import static org.junit.Assert.*;

public final class CLIParsersTest {

    @Test
    public void testIdentifierParser() {

        final Parser<Identifier> parser = CLIParsers.identifierParser;

        assertEquals(Identifier.of("abc"), parser.parse("abc"));
        assertEquals(Identifier.of("123-12"), parser.parse("123-12"));
        assertEquals(Identifier.of("abc"), parser.parse("abc"));
        assertEquals(Identifier.of("abcde_fgh"), parser.parse("abcde_fgh"));
        assertEquals(Identifier.of("abcde_-"), parser.parse("abcde_-"));
    }

    @Test
    public void testCommandParser() {

        final Parser<CLICommand> parser = CLIParsers.commandParser;

        assertEquals(InitCommand.of(), parser.parse(" init  "));

        assertEquals(RecipesCommand.of(), parser.parse(" recipes  "));

        assertEquals(UpgradeCommand.of(), parser.parse("upgrade"));

        assertEquals(
                InstallCommand.of(Identifier.of("awesome")),
                CLIParsers.commandParser.parse(" install   awesome  "));

        assertEquals(
                InstallCommand.of(Identifier.of("some_lib"), BoundedSemanticVersion.atLeast(SemanticVersion.of(2))),
                CLIParsers.commandParser.parse(" install  some_lib>=2  "));

        assertEquals(
                InstallCommand.of(Identifier.of("another-lib2"), ExactSemanticVersion.of(SemanticVersion.of(2))),
                CLIParsers.commandParser.parse("install another-lib2 [ 2 ]  "));

        assertEquals(
                UpdateCommand.of(Identifier.of("boost-config")),
                CLIParsers.commandParser.parse(" uPdAte  boost-config "));

        assertEquals(
                UpdateCommand.of(),
                CLIParsers.commandParser.parse("update"));

        try {
            parser.parse("installsomething");
            assertTrue(false);
        } catch (final ParserException e) {
            assertTrue(true);
        }
    }
}