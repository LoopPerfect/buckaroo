package com.loopperfect.buckaroo;

/**
 * Marker interface for objects that represent events that may
 * occur when Buckaroo is running a command.
 *
 * Examples of events might be:
 *  - a file was downloaded
 *  - a dependency was resolved
 *  - a Git clone finished
 */
public interface Event {

    // TODO: Implement the visitor pattern?
}
