package com.loopperfect.buckaroo.tasks;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;

public final class DownloadProgress extends Event {

    public final long downloaded;
    public final long contentLength;

    public boolean hasKnownContentLength() {
        return contentLength > 0;
    }

    public float progress() {
        Preconditions.checkState(hasKnownContentLength());
        return clamp(downloaded / (float) contentLength, 0f, 1f);
    }

    private DownloadProgress(final long downloaded, final long contentLength) {
        Preconditions.checkArgument(downloaded >= 0L);
        // TODO: Support the no content-length case better
        Preconditions.checkArgument(contentLength == -1L || contentLength >= 0L);
        Preconditions.checkArgument(contentLength == -1L || downloaded <= contentLength);
        this.downloaded = downloaded;
        this.contentLength = contentLength;
    }

    public boolean equals(final DownloadProgress other) {
        Preconditions.checkNotNull(other);
        return downloaded == other.downloaded &&
            contentLength == other.contentLength;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(downloaded) * Long.hashCode(contentLength) * super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof DownloadProgress &&
            equals((DownloadProgress) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("date", date)
            .add("threadId", threadId)
            .add("downloaded", downloaded)
            .add("contentLength", contentLength)
            .toString();
    }

    public static DownloadProgress of(final long downloaded, final long contentLength) {
        return new DownloadProgress(downloaded, contentLength);
    }

    private static float clamp(final float a, final float min, final float max) {
        if (a < min) {
            return min;
        }
        if (a > max) {
            return max;
        }
        return a;
    }
}
