package com.loopperfect.buckaroo;

import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public final class SimplePathTest {

    @Test
    public void equals() {

        final SimplePath p = SimplePath.of(".", "a", "b", "c");
        final SimplePath q = SimplePath.of(".", "a", "b", "c");
        final SimplePath r = SimplePath.of("a", "b", "c");

        assertTrue(p.equals(q));
        assertTrue(p.equals((Object)q));

        assertFalse(p.equals(r));
        assertFalse(p.equals((Object)r));
        assertFalse(p.equals((Object)null));
    }

    @Test
    public void name() {

        final SimplePath p = SimplePath.of(".", "a", "b", "c");

        assertEquals("c", p.name());
    }

    @Test
    public void append() {

        assertEquals(
            SimplePath.of("a", "b", "c", "d"),
            SimplePath.of("a").append("b").append("c", "d"));
    }

    @Test
    public void toPath() throws IOException {

        final FileSystem fs = Jimfs.newFileSystem();

        Files.createDirectories(fs.getPath("a", "b", "c"));
        Files.createFile(fs.getPath("a", "b", "c", "hello.txt"));

        final SimplePath p = SimplePath.of("a").append("b").append("c").append("hello.txt");

        assertTrue(Files.exists(p.toPath(fs)));
    }

    @Test
    public void fromPath() throws IOException {

        final FileSystem fs = Jimfs.newFileSystem();

        Files.createDirectories(fs.getPath("a", "b", "c"));

        final Path path = fs.getPath("a/b/c/");

        assertEquals(path, SimplePath.of(path).toPath(path.getFileSystem()));
    }
}