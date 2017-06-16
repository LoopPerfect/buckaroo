package com.loopperfect.buckaroo.github;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Project;

public final class FetchReleaseEvent extends Event {

    public final Either<Event, Project> value;

    public boolean isDone() {
        return value.right().isPresent();
    }

    private FetchReleaseEvent(final Either<Event, Project> value) {
        Preconditions.checkNotNull(value);
        this.value = value;
    }

    // TODO: equals, hashCode

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("value", value)
            .toString();
    }

    public static FetchReleaseEvent progress(final Event progress) {
        return new FetchReleaseEvent(Either.left(progress));
    }

    public static FetchReleaseEvent result(final Project project) {
        return new FetchReleaseEvent(Either.right(project));
    }
}
