package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;

public final class RemoteCookBook {

    public final Identifier name;
    public final String url;

    private RemoteCookBook(final Identifier name, final String url) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(url);
        this.name = name;
        this.url = url;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof RemoteCookBook)) {
            return false;
        }

        final RemoteCookBook other = (RemoteCookBook) obj;

        return Objects.equals(name, other.name) &&
            Objects.equals(url, other.url);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("url", url)
            .toString();
    }

    public static RemoteCookBook of(final Identifier name, final String url) {
        return new RemoteCookBook(name, url);
    }
}
