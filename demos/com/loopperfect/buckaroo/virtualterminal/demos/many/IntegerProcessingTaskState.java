package com.loopperfect.buckaroo.virtualterminal.demos.many;

import com.google.common.collect.ImmutableList;

public final class IntegerProcessingTaskState implements TaskState {

    public final ImmutableList<Integer> processed;
    public final ImmutableList<Integer> pending;

    public IntegerProcessingTaskState(final ImmutableList<Integer> processed, final ImmutableList<Integer> pending) {
        this.processed = processed;
        this.pending = pending;
    }
}
