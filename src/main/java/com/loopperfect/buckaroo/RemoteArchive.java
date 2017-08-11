package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;

import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public final class RemoteArchive {

    public final URI url;
    public final HashCode sha256;
    public final Optional<String> subPath;

    private RemoteArchive(final URI url, final HashCode sha256, final Optional<String> subPath) {
        super();
        this.url = Preconditions.checkNotNull(url);
        this.sha256 = Preconditions.checkNotNull(sha256);
        this.subPath = Preconditions.checkNotNull(subPath);
    }

    public RemoteFile asRemoteFile() {
        return RemoteFile.of(url, sha256);
    }

    public boolean equals(final RemoteArchive other) {
        Preconditions.checkNotNull(other);
        return url.equals(other.url) &&
            sha256.equals(other.sha256) &&
            subPath.equals(other.subPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, sha256, subPath);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof RemoteArchive && equals((RemoteArchive) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("url", url)
            .add("sha256", sha256)
            .add("subPath", subPath)
            .toString();
    }

    public static RemoteArchive of(final URI url, final HashCode sha256, final Optional<String> subPath) {
        return new RemoteArchive(url, sha256, subPath);
    }

    public static RemoteArchive of(final URI url, final HashCode sha256, final String subPath) {
        return new RemoteArchive(url, sha256, Optional.of(subPath));
    }

    public static RemoteArchive of(final URI url, final HashCode sha256) {
        return new RemoteArchive(url, sha256, Optional.empty());
    }
}
