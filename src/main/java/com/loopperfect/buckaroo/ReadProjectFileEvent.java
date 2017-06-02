package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public final class ReadProjectFileEvent implements Event {

    public final Project project;

    private ReadProjectFileEvent(final Project project) {
        Preconditions.checkNotNull(project);
        this.project = project;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("project", project)
            .toString();
    }

    public static ReadProjectFileEvent of(final Project project) {
        return new ReadProjectFileEvent(project);
    }
}
