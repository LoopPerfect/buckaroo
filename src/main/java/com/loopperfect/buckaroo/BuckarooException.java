package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

public class BuckarooException extends Exception {

    public BuckarooException(final String message, final Throwable exception) {
        super(Preconditions.checkNotNull(message), Preconditions.checkNotNull(exception));
    }

    public BuckarooException(final Throwable exception) {
        super(Preconditions.checkNotNull(exception));
    }

    public BuckarooException(final String message) {
        super(Preconditions.checkNotNull(message));
    }

    public BuckarooException() {
        super();
    }
}
