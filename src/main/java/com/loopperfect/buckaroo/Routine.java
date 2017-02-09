package com.loopperfect.buckaroo;

@FunctionalInterface
public interface Routine {

    void execute() throws BuckarooException;
}
