package com.loopperfect.reflex;

import com.google.common.base.Preconditions;

import java.util.function.Consumer;
import java.util.function.Function;

public final class StateStore<T> {

    private final Object LOCK = new Object();

    private final Consumer<T> updateCallback;
    private volatile T state;

    private StateStore(final T initialState, final Consumer<T> updateCallback) {

        Preconditions.checkNotNull(initialState);
        Preconditions.checkNotNull(updateCallback);

        this.state = initialState;
        this.updateCallback = updateCallback;
    }

    public void update(final T nextState) {
        Preconditions.checkNotNull(nextState);
        synchronized (LOCK) {
            if (!state.equals(nextState)) {
                this.state = nextState;
            }
        }
        updateCallback.accept(state);
    }

    public void update(final Function<T, T> f) {
        Preconditions.checkNotNull(state);
        synchronized (LOCK) {
            final T nextState = f.apply(state);
            if (!this.state.equals(nextState)) {
                this.state = nextState;
            }
        }
        updateCallback.accept(state);
    }

    public static <T> StateStore<T> of(final T initialState, final Consumer<T> updateCallback) {
        return new StateStore<T>(initialState, updateCallback);
    }
}
