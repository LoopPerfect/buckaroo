package com.loopperfect.buckaroo.virtualterminal.demos.many;

import com.loopperfect.buckaroo.reflex.StateStore;

import java.util.Stack;

public final class Main {

    private Main() {
        super();
    }

    public static void main(final String[] args) throws InterruptedException {

        // Create a container for the application state.
        // The callback will be fired every time the state changes.
        final StateStore<Stack<TaskState>> stateStore = StateStore.of(
            new Stack<>(),
            state -> {
                System.out.println(state);
            });
    }
}
