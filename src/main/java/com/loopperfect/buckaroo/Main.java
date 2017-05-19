package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.loopperfect.buckaroo.cli.CLICommand;
import com.loopperfect.buckaroo.cli.CLIParsers;
import com.loopperfect.buckaroo.io.IOContext;
import com.loopperfect.buckaroo.routines.Routines;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jparsec.Parser;
import org.jparsec.error.ParserException;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

public final class Main {

    private Main() {

    }

    public static void main(final String[] args) {

        if (args.length == 0) {
            System.out.println("Buck, Buck, Buckaroo! \uD83E\uDD20");
            System.out.println("https://buckaroo.readthedocs.io/");
            return;
        }

        final String rawCommand = String.join(" ", Arrays.stream(args).map(String::trim).toArray(String[]::new));
        final IOContext context = IOContext.actual();

        // Send the command to the logging server, if present
        final Either<IOException, BuckarooConfig> loadConfigResult = Routines.loadConfig.apply(context);

        if (loadConfigResult.right().isPresent() && loadConfigResult.right().get().analyticsServer.isPresent()) {
            final URL analyticsServer = loadConfigResult.right().get().analyticsServer.get();
            try {
                post(analyticsServer, generateLogString(context, rawCommand));
            } catch (final Exception ignored) {
                // A failure to log should not be reported to the user.
            }
        }

        // Parse the command
        final Parser<CLICommand> commandParser = CLIParsers.commandParser;

        try {
            final CLICommand command = commandParser.parse(rawCommand);
            command.routine().apply(context);
        } catch (final ParserException e) {
            System.out.println("Uh oh!");
            System.out.println(e.getMessage());
        }
    }

    private static void post(final URL url, final String data) throws Exception {

        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(data);

        final String charset = "UTF-8";

        final HttpClient httpClient = HttpClientBuilder.create().build();

        final HttpPost request = new HttpPost(url.toURI());
        final StringEntity params = new StringEntity(data);

        request.addHeader("Content-Type", "application/json; charset=" + charset);
        request.setEntity(params);

        final HttpResponse response = httpClient.execute(request);
    }

    private static String generateLogString(final IOContext context, final String command) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(command);

        // Body
        final JsonObject body = new JsonObject();

        // Session
        body.addProperty("session", Routines.getIdentifier.apply(context));

        // App
        body.addProperty("app", "buckaroo-cli");

        // Event
        body.addProperty("event", "command");

        // Data
        final JsonObject data = new JsonObject();

        data.addProperty("version", Buckaroo.version.encode());
        data.addProperty("os", System.getProperty("os.name"));
        data.addProperty("command", command);

        body.add("data", data);

        return body.toString();
    }
}
