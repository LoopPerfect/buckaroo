package com.loopperfect.buckaroo.virtualterminal.demos.downloader;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class BoringMain {

    private BoringMain() {
        super();
    }

    public static void main(final String[] args) throws InterruptedException {

        final List<String> urls = ImmutableList.of(
            "https://github.com/wix/react-native-calendars/archive/v1.2.13.zip",
            "https://github.com/funretro/distributed/archive/3.1.zip",
            "https://github.com/assimp/assimp/archive/v3.3.1.zip");

        System.out.println("Downloading " + urls.size() + " file(s)... ");

        for (final String url : urls) {
            System.out.println("Downloading " + url + "...");

            for (int i = 0; i < 10; i++) {
                Thread.sleep(100);
                System.out.println(i * 10 + "%");
            }

            System.out.println("100%");
            System.out.println("Done. ");
        }

        System.out.println("Downloaded " + urls.size() + " file(s). ");
    }
}
