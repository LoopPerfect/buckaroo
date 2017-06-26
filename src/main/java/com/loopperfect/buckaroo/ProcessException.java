package com.loopperfect.buckaroo;

public final class ProcessException extends Exception {

    public ProcessException() {
        super();
    }

    public ProcessException(final Throwable innerException) {
        super(innerException);
    }

    public ProcessException(final String message, final Throwable innerException) {
        super(message, innerException);
    }
}
