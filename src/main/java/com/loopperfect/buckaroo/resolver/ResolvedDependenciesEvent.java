package com.loopperfect.buckaroo.resolver;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.ResolvedDependencies;

public final class ResolvedDependenciesEvent implements Event {

    public final ResolvedDependencies dependencies;

    private ResolvedDependenciesEvent(final ResolvedDependencies dependencies) {
        Preconditions.checkNotNull(dependencies);
        this.dependencies = dependencies;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("dependencies", dependencies)
            .toString();
    }

    public static ResolvedDependenciesEvent of(final ResolvedDependencies dependencies) {
        return new ResolvedDependenciesEvent(dependencies);
    }
}
