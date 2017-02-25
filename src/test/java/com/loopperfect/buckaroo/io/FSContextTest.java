package com.loopperfect.buckaroo.io;

import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

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
}
