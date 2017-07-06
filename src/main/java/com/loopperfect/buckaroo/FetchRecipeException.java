package com.loopperfect.buckaroo;

public class FetchRecipeException extends Exception {

    public FetchRecipeException(final String message) {
        super(message);
    }

    public FetchRecipeException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public FetchRecipeException(final Throwable throwable) {
        super(throwable);
    }
}
