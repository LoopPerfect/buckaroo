package com.loopperfect.buckaroo;

@Deprecated
@FunctionalInterface
public interface Action {

    void run();

    static Action of(final Action action) {
        return action;
    }
}
