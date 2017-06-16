package com.loopperfect.buckaroo;

import com.google.common.base.Strings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestUtils {

    private TestUtils() {

    }

    public static void printTree(final Path directory) throws IOException {
        printTree(directory, 0);
    }

    private static void printTree(final Path directory, final int indentation) throws IOException {
        for (final Path path : (Iterable<Path>) Files.list(directory)::iterator) {
            System.out.println(Strings.repeat(" ", indentation * 2) + path);
            if (Files.isDirectory(path)) {
                printTree(path, indentation + 1);
            }
        }
    }
}
