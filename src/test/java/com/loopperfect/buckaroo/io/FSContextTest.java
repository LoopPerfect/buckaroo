package com.loopperfect.buckaroo.io;

import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.Either;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class FSContextTest {

    @Test
    public void testWriteAndExists() {

        final FSContext context = FSContext.fake();

        final Optional<IOException> result =
                context.writeFile("./test.txt", "Hello, world. ", true);

        assertTrue(!result.isPresent());

        assertTrue(context.exists("./test.txt"));
    }

    @Test
    public void testOverwriteAndExists() {

        final FSContext context = FSContext.fake();

        assertTrue(!context.exists("./test.txt"));

        final Optional<IOException> result1 =
                context.writeFile("./test.txt", "Hello, world. ", false);

        assertTrue(!result1.isPresent());

        final Optional<IOException> result2 =
                context.writeFile("./test.txt", "Hello, world. ", false);

        assertTrue(result2.isPresent());

        final Optional<IOException> result3 =
                context.writeFile("./test.txt", "Hello, world. ", true);

        assertTrue(!result3.isPresent());

        assertTrue(context.exists("./test.txt"));
    }

    @Test
    public void testNestedFileWrite() {

        final FSContext context = FSContext.fake();

        assertTrue(!context.exists("./nested/file.txt"));

        final Optional<IOException> result =
                context.writeFile("./nested/file.txt", "Hello, world. ", true);

        assertTrue(!result.isPresent());

        assertTrue(context.exists("./nested/file.txt"));
    }

    @Test
    public void testOverwriteClears() {

        final FSContext context = FSContext.fake();

        final Optional<IOException> result1 =
                context.writeFile("file.txt", "Hello, world. \n abc \n def \n ghi \n", true);

        assertTrue(!result1.isPresent());

        final Optional<IOException> result2 =
                context.writeFile("file.txt", "abc", true);

        assertTrue(!result2.isPresent());

        final Either<IOException, String> result3 = context.readFile("file.txt");

        assertEquals(Either.right("abc"), result3);
    }

    @Test
    public void testListFiles() {

        final FSContext context = FSContext.fake();

        context.createDirectory("a");
        context.createDirectory(context.getPath("a", "b").toString());
        context.createDirectory(context.getPath("a", "b", "c").toString());

        context.writeFile(
            context.getPath("a", "b", "c", "test.txt").toString(),
            "Hello, world. ");

        assertEquals(
            Either.right(ImmutableList.of("a/b")),
            context.listFiles("a"));

        assertEquals(
            Either.right(ImmutableList.of("a/b/c")),
            context.listFiles(context.getPath("a", "b").toString()));

        assertEquals(
            Either.right(ImmutableList.of("a/b/c/test.txt")),
            context.listFiles(context.getPath("a", "b", "c").toString()));
    }
}
