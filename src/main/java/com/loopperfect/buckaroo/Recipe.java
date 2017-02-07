package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public final class Recipe {

    public final Identifier identifier;
    public final String url;
    public final ImmutableMap<SemanticVersion, String> versions;

    public Recipe(final Identifier identifier, final String url, final ImmutableMap<SemanticVersion, String> versions) {

        super();

        this.identifier = Preconditions.checkNotNull(identifier);
        this.url = Preconditions.checkNotNull(url);
        this.versions = Preconditions.checkNotNull(versions);
    }
}
