package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public final class BuckarooConfig {

    public final ImmutableList<RemoteCookBook> cookBooks;
    public final Optional<URL> analyticsServer;

    private BuckarooConfig(final ImmutableList<RemoteCookBook> cookBooks, final Optional<URL> analyticsServer) {
        this.cookBooks = Preconditions.checkNotNull(cookBooks);
        this.analyticsServer = Preconditions.checkNotNull(analyticsServer);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof BuckarooConfig)) {
            return false;
        }
        final BuckarooConfig other = (BuckarooConfig) obj;
        return Objects.equals(cookBooks, other.cookBooks) &&
            Objects.equals(analyticsServer, other.analyticsServer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cookBooks, analyticsServer);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("cookBooks", cookBooks)
            .add("analyticsServer", analyticsServer)
            .toString();
    }

    public static BuckarooConfig of(final ImmutableList<RemoteCookBook> cookBooks, final Optional<URL> analyticsServer) {
        return new BuckarooConfig(cookBooks, analyticsServer);
    }

    public static BuckarooConfig of(final ImmutableList<RemoteCookBook> cookBooks, final URL analyticsServer) {
        return new BuckarooConfig(cookBooks, Optional.of(analyticsServer));
    }

    public static BuckarooConfig of(final ImmutableList<RemoteCookBook> cookBooks) {
        return new BuckarooConfig(cookBooks, Optional.empty());
    }
}
