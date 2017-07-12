package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.CheckedSupplier;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.net.URL;

public final class PostTask {

    private PostTask() {

    }

    public static Single<Response> post(final CheckedSupplier<OkHttpClient, Exception> clientFactory, final URL url, final RequestBody requestBody) {

        Preconditions.checkNotNull(clientFactory);
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(requestBody);

        return Single.fromCallable(() -> {

            final OkHttpClient client = clientFactory.get();

            final Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

            return client.newCall(request).execute();
        }).subscribeOn(Schedulers.io());
    }

    public static Single<Response> post(final URL url, final RequestBody requestBody) {
        return post(() -> new OkHttpClient.Builder().followRedirects(true).build(), url, requestBody);
    }
}
