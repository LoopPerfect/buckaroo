package com.loopperfect.buckaroo;

import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;

public final class EvenMoreFilesTest {

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
        try (final FileSystem zipfs = FileSystems.newFileSystem(fs.getPath("stuff.zip"), null)) {

            Files.copy(fs.getPath("hello.txt"), zipfs.getPath("message.txt"),
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
        try (final FileSystem zipfs = FileSystems.newFileSystem(fs.getPath("stuff.zip"), null)) {

            Files.createDirectories(zipfs.getPath("a", "b", "c"));

            Files.copy(fs.getPath("hello.txt"), zipfs.getPath("a", "b", "c", "message.txt"),
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
}