package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import com.loopperfect.buckaroo.Buckaroo;
import com.loopperfect.buckaroo.CheckedSupplier;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.events.PostRequestEvent;
import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ServiceConfigurationError;
import java.util.UUID;

public final class LoggingTasks {

    private LoggingTasks() {

    }

    public static Observable<Event> log(final FileSystem fs, final String command) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(command);

        final CheckedSupplier<OkHttpClient, Exception> factory = () -> {
            final SSLContext context = getLetsEncryptContext();
            return new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .sslSocketFactory(context.getSocketFactory())
                .build();
        };

        // Read the config to get the analytics server
        return CommonTasks.readAndMaybeGenerateConfigFile(fs).flatMapObservable(

            // Send the log data
            readConfigFileEvent -> SessionTasks.readOrGenerateSessionId(fs).flatMapObservable(session -> {

                if (readConfigFileEvent.config.analyticsServer.isPresent()) {
                    return PostTask.post(
                        factory,
                        readConfigFileEvent.config.analyticsServer.get(),
                        RequestBody.create(
                            MediaType.parse("application/json"),
                            generateLogString(session, command)))
                        .map(ignored -> PostRequestEvent.of())
                        .toObservable();
                }

                return Observable.error(() -> new ServiceConfigurationError(
                    "No analytics server was found in the configuration file. "));

            })
        );
    }

    // Java does not verify Let's Encrypt certificates by default.
    // Instead, we ship with their root certificate and add it at run-time.
    private static SSLContext getLetsEncryptContext() throws Exception {

        final URL url = Resources.getResource("DSTRootCAX3.crt");
        final ByteSource byteSource = Resources.asByteSource(url);

        try (final InputStream inputStream = byteSource.openBufferedStream()) {

            final Certificate certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(inputStream);

            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            keyStore.load(null, null);
            keyStore.setCertificateEntry(Integer.toString(1), certificate);

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());

            tmf.init(keyStore);

            final SSLContext context = SSLContext.getInstance("TLS");

            context.init(null, tmf.getTrustManagers(), null);

            return context;
        }
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
