package com.loopperfect.buckaroo;

import java.util.Iterator;
import java.util.Objects;
import java.nio.file.Path;

public final class MorePaths {

    private MorePaths() {

    }

    public static Iterable<String> names(final Path path) {
        Objects.requireNonNull(path);
        return () -> new Iterator<String>() {
            int index = 0;
            @Override
            public boolean hasNext() {
                return index < path.getNameCount();
            }
            @Override
            public String next() {
                index++;
                return path.getName(index - 1).getFileName().toString();
            }
        };
    }
}
