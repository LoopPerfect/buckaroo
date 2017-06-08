package com.loopperfect.buckaroo.virtualterminal.demos.downloader;

import com.loopperfect.buckaroo.reflex.StateStore;

import java.util.function.Consumer;

public final class Main {

    private Main() {
        super();
    }

    private static void download(final String url, final Consumer<Float> callback) throws InterruptedException {

        // Simulate downloading a file.
        // Could be in another thread.
        for (int i = 0; i < 10; i++) {
            callback.accept(i / 10f);
            Thread.sleep(100);
        }

        callback.accept(1f);
    }

    public static void main(final String[] args) throws InterruptedException {

        // Create a container for the application state.
        // The callback will be fired every time the state changes.
        final StateStore<DownloadState> stateStore = StateStore.of(
            new DownloadState(),
            state -> {
                // Render to the console.
                // Here is where we would wire-up virtual-terminal.
                if (state.progress == 0f) {
                    System.out.println("Downloading " + state.url + "...");
                }

                System.out.println(state.progress * 100f + "%");

                if (state.progress == 1f) {
                    System.out.println("Done. ");
                }
            });

        download("https://github.com/assimp/assimp/archive/v3.3.1.zip", progress -> {
            stateStore.update(s -> new DownloadState(s.url, progress));
        });
    }
}
