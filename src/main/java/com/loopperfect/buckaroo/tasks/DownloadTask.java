package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class DownloadTask {

    private static final int PROGRESS_REPORT_EVERY_N_BYTES = 1024;

    private DownloadTask() {

    }

    public static Single<String> download(final URL url) {
        Preconditions.checkNotNull(url);
        return Single.fromCallable(() -> {
            final URLConnection conn = url.openConnection();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        });
    }

    public static Observable<DownloadProgress> download(final URL url, final Path target) {

        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(target);

        return Single.fromCallable(() -> {

            if (Files.exists(target)) {
                throw new IOException("There is already a file at " + target + ". ");
            }

            if (!Files.exists(target.getParent())) {
                Files.createDirectories(target.getParent());
            }

            return Files.newOutputStream(target);
        }).flatMapObservable(outputStream -> download(url, outputStream));
    }

    public static Observable<DownloadProgress> download(final URL url, final OutputStream output) {

        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(output);

        return Observable.create(emitter -> {

            final OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder()
                .url(url)
                .build();

            final Response response = client.newCall(request).execute();

            try {

                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                final InputStream input = new BufferedInputStream(response.body().byteStream());

                final byte[] data = new byte[1024];
                final long contentLength = response.body().contentLength();

                long total = 0;

                emitter.onNext(DownloadProgress.of(total, contentLength));

                int count;
                long lastCount = 0;
                long lastEmissionCount = 0;
                while ((count = input.read(data)) != -1) {

                    total += count;
                    output.write(data, 0, count);

                    if ((total - lastCount) >= PROGRESS_REPORT_EVERY_N_BYTES) {
                        lastEmissionCount = total;
                        emitter.onNext(DownloadProgress.of(total, contentLength));
                    }
                    lastCount = total;
                }

                output.flush();
                output.close();

                input.close();

                if (lastEmissionCount != total) {
                    emitter.onNext(DownloadProgress.of(total, contentLength));
                }
                emitter.onComplete();
            } catch (final Throwable e) {
                emitter.onError(e);
            }

            emitter.onComplete();
        });
    }
}
