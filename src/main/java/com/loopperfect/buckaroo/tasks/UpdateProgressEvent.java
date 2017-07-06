package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RemoteCookbook;

/**
 * Created by gaetano on 06/07/17.
 */
public class UpdateProgressEvent extends Event {
    public final ImmutableMap<RemoteCookbook, Event> progress;

    private UpdateProgressEvent(final ImmutableMap<RemoteCookbook, Event> progress) {
        this.progress = Preconditions.checkNotNull(progress);
    }

    //TODO: implement hashCode etc...

    @Override
    public String toString() {
        return progress.toString();
    }

    public static UpdateProgressEvent of(final ImmutableMap<RemoteCookbook, Event> progress) {
        return new UpdateProgressEvent(progress);
    }
}
