package com.loopperfect.buckaroo.tasks;

@FunctionalInterface
public interface UnsafeRunnable {

    void run() throws Throwable;
}
