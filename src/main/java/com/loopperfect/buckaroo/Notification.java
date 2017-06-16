package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import java.util.Objects;

public final class Notification extends Event {

    public final String message;

    private Notification(final String message) {
        Preconditions.checkNotNull(message);
        this.message = message;
    }

    public boolean equals(final Notification other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && obj instanceof Notification) {
            return equals((Notification)obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("message", message)
            .toString();
    }

    public static Notification of(final String message) {
        return new Notification(message);
    }
}
