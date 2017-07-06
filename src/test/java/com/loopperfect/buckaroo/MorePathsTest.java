package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.junit.Assert.*;

public final class MorePathsTest {

    @Test
    public void names1() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Path path = fs.getPath("a", "b", "c", "d");

        final ImmutableList<String> expected = ImmutableList.of("a", "b", "c", "d");
        final ImmutableList<String> actual = ImmutableList.copyOf(MorePaths.names(path));

        assertEquals(expected, actual);
    }

    @Test
    public void names2() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Path path = fs.getPath("");

        final ImmutableList<String> expected = ImmutableList.of("");
        final ImmutableList<String> actual = ImmutableList.copyOf(MorePaths.names(path));

        assertEquals(expected, actual);
    }
}