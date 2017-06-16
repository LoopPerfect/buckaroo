package com.loopperfect.buckaroo;

import com.google.common.base.Charsets;
import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class EvenMoreFilesTest {

    @Test
    public void copyDirectory() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final String expected = "Hello, world";

        Files.createDirectories(fs.getPath("a", "b", "c"));
        Files.write(fs.getPath("a", "b", "hello.txt"), expected.getBytes(Charsets.UTF_8));

        EvenMoreFiles.copyDirectory(fs.getPath("a"), fs.getPath("x"));

        assertTrue(Files.exists(fs.getPath("x")));
        assertTrue(Files.exists(fs.getPath("x", "b", "c")));
        assertTrue(Files.exists(fs.getPath("x", "b", "hello.txt")));

        final String actual = Charsets.UTF_8.decode(ByteBuffer.wrap(
            Files.readAllBytes(fs.getPath("x", "b", "hello.txt")))).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void unzip1() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final String expected = "Hello, world. ";

        // Write a test file.
        Files.write(fs.getPath("hello.txt"), expected.getBytes());

        // Create the zip.
        {
            final ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(fs.getPath("stuff.zip")));

            zipOutputStream.finish();
            zipOutputStream.close();
        }

        // Add the test file to the zip.
        try (final FileSystem zipFileSystem = EvenMoreFiles.zipFileSystem(fs.getPath("stuff.zip"))) {

            Files.copy(fs.getPath("hello.txt"), zipFileSystem.getPath("message.txt"),
                StandardCopyOption.REPLACE_EXISTING);
        }

        // Unpack the zip!
        EvenMoreFiles.unzip(fs.getPath("stuff.zip"), fs.getPath("stuff"), Optional.empty());

        // Verify the unzipped file matches the test file
        final byte[] bytes = Files.readAllBytes(fs.getPath("stuff", "message.txt"));
        final String actual = Charset.defaultCharset().decode(ByteBuffer.wrap(bytes)).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void unzip2() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final String expected = "Hello, world. ";

        // Write a test file.
        Files.write(fs.getPath("hello.txt"), expected.getBytes());

        // Create the zip.
        {
            final ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(fs.getPath("stuff.zip")));

            zipOutputStream.finish();
            zipOutputStream.close();
        }

        // Add the test file to the zip.
        try (final FileSystem zipFileSystem = EvenMoreFiles.zipFileSystem(fs.getPath("stuff.zip"))) {

            Files.createDirectories(zipFileSystem.getPath("a", "b", "c"));

            Files.copy(fs.getPath("hello.txt"), zipFileSystem.getPath("a", "b", "c", "message.txt"),
                StandardCopyOption.REPLACE_EXISTING);
        }

        // Unpack the zip!
        EvenMoreFiles.unzip(
            fs.getPath("stuff.zip"),
            fs.getPath("stuff"),
            Optional.of(fs.getPath(fs.getSeparator(), "a", "b")));

        // Verify the unzipped file matches the test file
        final byte[] bytes = Files.readAllBytes(fs.getPath("stuff", "c", "message.txt"));
        final String actual = Charset.defaultCharset().decode(ByteBuffer.wrap(bytes)).toString();

        assertEquals(expected, actual);
    }

    @Test
    public void writeAndReadFile() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final String expected = "Hello, world. \nHow are you?\n\n\nend.";
        final Path path = fs.getPath("a", "b", "c", "test.txt").toAbsolutePath();

        EvenMoreFiles.writeFile(path, expected);

        final String actual = EvenMoreFiles.read(path);

        assertEquals(expected, actual);
    }

    @Test(expected=IOException.class)
    public void writeDoesNotOverwrite() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();
        final Path path = fs.getPath("test.txt");

        EvenMoreFiles.writeFile(path, "Testing... testing...");
        EvenMoreFiles.writeFile(path, "123");
    }
}