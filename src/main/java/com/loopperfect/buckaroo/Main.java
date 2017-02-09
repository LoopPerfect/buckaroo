package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;

public final class Main {

    public static void main(final String[] args) {

        if (args.length == 0) {
            System.out.println("Buck, Buck, Buckaroo! \uD83E\uDD20");
            return;
        }

        final Parser<CLICommand> commandParser = CLIParsers.commandParser;

        try {
            final CLICommand command = commandParser.parse(String.join(" ", args));

            command.routine().execute();
        } catch (final ParserException | BuckarooException e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        }
    }
}
