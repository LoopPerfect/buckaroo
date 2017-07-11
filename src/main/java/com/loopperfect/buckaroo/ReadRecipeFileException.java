package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;

import java.nio.file.Path;
import java.util.Objects;

public final class ReadRecipeFileException extends Exception {

    public final Path path;
    public final Throwable cause;

    public ReadRecipeFileException(final Path path, final Throwable cause) {
        super(cause);
        Objects.requireNonNull(path);
        Objects.requireNonNull(cause);
        this.path = path;
        this.cause = cause;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .addValue(path)
            .add("cause", cause)
            .toString();
    }

    public static ReadRecipeFileException wrap(final Path path, final Throwable throwable) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(throwable);
        if (throwable instanceof ReadRecipeFileException) {
            final ReadRecipeFileException other = (ReadRecipeFileException) throwable;
            if (Objects.equals(path, other.path)) {
                return other;
            }
        }
        return new ReadRecipeFileException(path, throwable);
    }
}
