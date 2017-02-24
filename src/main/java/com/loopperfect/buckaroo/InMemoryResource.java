package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;
import java.util.Objects;

public final class InMemoryResource implements Resource {

    private final String content;

    private InMemoryResource(final String content) {
        Preconditions.checkNotNull(content);
        this.content = content;
    }

    @Override
    public String description() {
        return "{" +
                (content.length() > 27 ?
                        content.substring(0, 30) + "..." :
                        content) +
                "}";
    }

    @Override
    public IO<Either<IOException, String>> fetch() {
        return IO.value(Either.right(content));
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof InMemoryResource)) {
            return false;
        }

        final InMemoryResource other = (InMemoryResource) obj;

        return Objects.equals(content, other.content);
    }

    public static Resource of(final String content) {
        return new InMemoryResource(content);
    }
}
