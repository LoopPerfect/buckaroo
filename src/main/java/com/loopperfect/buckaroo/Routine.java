package com.loopperfect.buckaroo;

import java.util.Optional;

public interface Routine<E extends Throwable> {

    <T extends Exception> Optional<T> execute();
}
