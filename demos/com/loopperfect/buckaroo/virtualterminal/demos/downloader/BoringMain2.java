package com.loopperfect.buckaroo.virtualterminal.demos.downloader;

import java.util.function.Consumer;

public final class BoringMain2 {

    private BoringMain2() {
        super();
    }

    private static void download(final String url, final Consumer<Float> callback) throws InterruptedException {

        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            callback.accept(i / 10f);
        }

        callback.accept(1f);
    }

    public static void main(final String[] args) throws InterruptedException {

        final String url = "https://github.com/assimp/assimp/archive/v3.3.1.zip";

        download(url, progress -> {

            if (progress == 0f) {
                System.out.println("Downloading " + url + "...");
            }

            System.out.println(progress * 100f + "%");

            if (progress == 1f) {
                System.out.println("Done. ");
            }
        });
    }
}
