package com.loopperfect.buckaroo.virtualterminal.demos.many;

import com.google.common.collect.ImmutableList;

public final class EmailSendingTaskState implements TaskState {

    public final ImmutableList<String> processed;
    public final ImmutableList<String> pending;

    public EmailSendingTaskState(final ImmutableList<String> processed, final ImmutableList<String> pending) {
        this.processed = processed;
        this.pending = pending;
    }
}
