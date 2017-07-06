package com.loopperfect.buckaroo;

/**
 * Created by gaetano on 06/07/17.
 */
public class CookbookUpdateException extends Exception{
    public CookbookUpdateException(final String message) {
        super(message);
    }

    public CookbookUpdateException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public CookbookUpdateException(final Throwable throwable) {
        super(throwable);
    }
}
