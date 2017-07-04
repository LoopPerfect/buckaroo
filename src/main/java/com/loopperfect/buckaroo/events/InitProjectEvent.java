package com.loopperfect.buckaroo.events;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Project;

/**
 * Created by gaetano on 04/07/17.
 */
public class InitProjectEvent extends Event {

    public final Project project;

    private InitProjectEvent(final Project project) {
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

    public static InitProjectEvent of(final Project project) {
        return new InitProjectEvent(project);
    }
}

