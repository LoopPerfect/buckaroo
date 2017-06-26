package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Process;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class DownloadTask {

    private static final int PROGRESS_REPORT_EVERY_N_BYTES = 1024;

    private DownloadTask() {

    }

    public static Process<Event, String> download(final URL url) {
        Preconditions.checkNotNull(url);
        return Process.of(Observable.using(
            ByteArrayOutputStream::new,
            byteArrayOutputStream -> Observable.concat(
                download(url, byteArrayOutputStream).map(Either::left),
                Observable.fromCallable(() -> Either.right(
                    new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())))),
            ByteArrayOutputStream::close));
    }

    public static Observable<DownloadProgress> download(final URL url, final Path target, final boolean overwrite) {

        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(target);

        return Single.fromCallable(() -> {

            final Path parent = target.getParent();

            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (overwrite) {
                return Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            return Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
        }).flatMapObservable(outputStream -> download(url, outputStream));
    }

    public static Observable<DownloadProgress> download(final URL url, final Path target) {
        return download(url, target, false);
    }

    public static Observable<DownloadProgress> download(final URL url, final OutputStream output) {

        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(output);

        final Observable<DownloadProgress> observable = Observable.create(emitter -> {

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

                    if(emitter.isDisposed()) break;

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

                if (emitter.isDisposed()) {
                    return;
                }

                if (lastEmissionCount != total) {
                    emitter.onNext(DownloadProgress.of(total, contentLength));
                }

                emitter.onComplete();
            } catch (final Throwable e) {
                if (emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });

        return observable.subscribeOn(Schedulers.io());
    }
}
