package com.loopperfect.buckaroo.virtualterminal.demos.downloader;

public final class DownloadState {

    public DownloadState() {

    }

    public DownloadState(final String url, final float progress) {
        this.url = url;
        this.progress = progress;
    }

    public String url = "";
    public float progress = 0f;
}
