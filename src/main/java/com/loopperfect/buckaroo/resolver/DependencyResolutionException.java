package com.loopperfect.buckaroo.resolver;

import com.google.common.base.Preconditions;

public class DependencyResolutionException extends Exception {

    public DependencyResolutionException() {
        super();
    }

    public DependencyResolutionException(final String message) {
        super(message);
        Preconditions.checkNotNull(message);
    }

    public DependencyResolutionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
