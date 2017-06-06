package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Project;

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
