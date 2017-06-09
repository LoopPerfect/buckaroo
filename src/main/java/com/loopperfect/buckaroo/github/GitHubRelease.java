package com.loopperfect.buckaroo.github;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.net.URL;

public final class GitHubRelease {

    public final String name;
    public final int id;
    public final String tagName;
    public final URL zipURL;

    private GitHubRelease(final String name, final int id, final String tagName, final URL zipURL) {

        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(tagName);
        Preconditions.checkNotNull(zipURL);

        this.name = name;
        this.id = id;
        this.tagName = tagName;
        this.zipURL = zipURL;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("id", id)
            .add("tagName", tagName)
            .add("zipURL", zipURL)
            .toString();
    }

    public static GitHubRelease of(final String name, final int id, final String tagName, final URL zipURL) {
        return new GitHubRelease(name, id, tagName, zipURL);
    }
}
