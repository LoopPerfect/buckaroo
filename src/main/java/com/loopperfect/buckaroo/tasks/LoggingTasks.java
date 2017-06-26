package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.loopperfect.buckaroo.Buckaroo;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.MoreObservables;
import com.loopperfect.buckaroo.events.PostRequestEvent;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URL;
import java.nio.file.FileSystem;
import java.util.ServiceConfigurationError;
import java.util.UUID;

public final class LoggingTasks {

    private LoggingTasks() {

    }

    public static Observable<Event> log(final FileSystem fs, final String command) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(command);

        // Read the config to get the analytics server
        return CommonTasks.readAndMaybeGenerateConfigFile(fs).flatMapObservable(

            // Send the log data
            readConfigFileEvent -> SessionTasks.readOrGenerateSessionId(fs).flatMap(session -> {

                if (readConfigFileEvent.config.analyticsServer.isPresent()) {
                    return post(
                        readConfigFileEvent.config.analyticsServer.get(),
                        generateLogString(session, command));
                }

                return Single.error(() -> new ServiceConfigurationError(
                    "No analytics server was found in the configuration file. "));

            }).toObservable().cast(Event.class)
        );
    }

    private static Single<PostRequestEvent> post(final URL url, final String data) {

        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(data);

        return Single.fromCallable(() -> {

            final String charset = "UTF-8";

            final HttpClient httpClient = HttpClientBuilder.create().build();

            final HttpPost request = new HttpPost(url.toURI());
            final StringEntity params = new StringEntity(data);

            request.addHeader("Content-Type", "application/json; charset=" + charset);
            request.setEntity(params);

            final HttpResponse response = httpClient.execute(request);

            return PostRequestEvent.of();
        });
    }

    private static String generateLogString(final UUID session, final String command) {

        Preconditions.checkNotNull(session);
        Preconditions.checkNotNull(command);

        // Body
        final JsonObject body = new JsonObject();

        // Session
        body.addProperty("session", session.toString());

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
