package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

public final class RemoteFile {

    public final URI url;
    public final HashCode sha256;

    private RemoteFile(final URI url, final HashCode sha256) {
        super();
        this.url = Preconditions.checkNotNull(url);
        this.sha256 = Preconditions.checkNotNull(sha256);
    }

    public boolean equals(final RemoteFile other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(url, other.url) &&
            Objects.equals(sha256, other.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, sha256);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof RemoteFile &&
            equals((RemoteFile) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("url", url)
            .add("sha256", sha256)
            .toString();
    }

    public static RemoteFile of(final URI url, final HashCode sha256) {
        return new RemoteFile(url, sha256);
    }
}
