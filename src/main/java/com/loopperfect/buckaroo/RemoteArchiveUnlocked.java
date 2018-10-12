package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public final class RemoteArchiveUnlocked {

    public final URI url;
    public final Optional<String> subPath;

    private RemoteArchiveUnlocked(final URI url, final Optional<String> subPath) {
        super();

        this.url = Preconditions.checkNotNull(url);
        this.subPath = Preconditions.checkNotNull(subPath);
    }

    public boolean equals(final RemoteArchiveUnlocked other) {
        Preconditions.checkNotNull(other);

        return url.equals(other.url) &&
            subPath.equals(other.subPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, subPath);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof RemoteArchive && equals((RemoteArchiveUnlocked) obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("url", url)
            .add("subPath", subPath)
            .toString();
    }

    public static RemoteArchiveUnlocked of(final URI url, final Optional<String> subPath) {
        return new RemoteArchiveUnlocked(url, subPath);
    }

    public static RemoteArchiveUnlocked of(final URI url, final String subPath) {
        return new RemoteArchiveUnlocked(url, Optional.of(subPath));
    }

    public static RemoteArchiveUnlocked of(final URI url) {
        return new RemoteArchiveUnlocked(url, Optional.empty());
    }
}
