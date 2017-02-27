package com.loopperfect.buckaroo;

/**
 * Created by gaetano on 16/02/17.
 */
public class DependencyResolverException extends BuckarooException {

    protected final Identifier id;

    protected DependencyResolverException(final Identifier id, final String message) {
        super(message);
        this.id = id;
    }

    public final Identifier getIdentifier() {
        return id;
    }

}
