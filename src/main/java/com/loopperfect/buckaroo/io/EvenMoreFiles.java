package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.SimplePath;

import java.io.*;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class EvenMoreFiles {

    private EvenMoreFiles() {
        super();
    }

    public static void writeFile(final Path path, final CharSequence content, final Charset charset, final boolean overwrite) throws IOException {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(content);
        Preconditions.checkNotNull(charset);
        if (Files.exists(path)) {
            if (!overwrite) {
                throw new IOException("There is already a file at " + path);
            }
            Files.delete(path);
        } else {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
        }
        Files.write(path, ImmutableList.of(content), charset, StandardOpenOption.CREATE);
    }

    public static void writeFile(final Path path, final CharSequence content) throws IOException {
        writeFile(path, content, Charset.defaultCharset(), false);
    }

    /*
     * Unzips a zip file at the given path into the given target path.
     * Optionally, a sub-path can be supplied which can be used to change the "root" of the zip file.
     *
     * For example:
     *
     *  source: myZip.zip
     *  target: myZip
     *  subPath: stuff
     *
     *  Input:
     *
     *    +--+ myZip.zip
     *       +--- file.txt
     *       +--+ stuff
     *          +--- hello.txt
     *          +--+ a
     *             +--- b.txt
     *
     *  Output:
     *
     *    +--+ myZip.zip
     *       +--- file.txt
     *       +--+ stuff
     *          +--- hello.txt
     *          +--+ a
     *             +--- b.txt
     *    +--+ myZip
     *       +--- hello.txt
     *       +--+ a
     *          +--- b.txt
     *
     *  Note how file.txt is not extracted because it is outside the sub-path, and hello.txt is at the root
     *  of the target directory.
     *
     */
    public static void unzip(final Path source, final Path target, final Optional<Path> subPath) throws IOException {
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }
        try (final ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(source))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                final ZipEntry current = entry;
                // Check that the current entry lives in the sub-path (or there is no sub-path!)
                if (subPath.map(x -> current.getName().startsWith(x.toString())).orElse(true)) {
                    final Path filePath = Paths.get(
                        target.toString(),
                        current.getName().substring(subPath.map(x -> x.toString().length()).orElse(0)));
                    if (current.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        extractFile(zipIn, filePath);
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private static final int BUFFER_SIZE = 4096;

    private static void extractFile(final ZipInputStream zipIn, final Path path) throws IOException {
        try (final OutputStream bos = new BufferedOutputStream(Files.newOutputStream(path))) {
            final byte[] data = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(data)) != -1) {
                bos.write(data, 0, read);
            }
            bos.flush();
            bos.close();
        }
    }

    public static Either<IOException, ByteChannel> openByteChannel(
        final Path path, Set<? extends OpenOption > options, FileAttribute<?>... attrs) {
        Preconditions.checkNotNull(path);
        try {
            final ByteChannel channel = path.getFileSystem()
                .provider()
                .newByteChannel(path, options, attrs);
            return Either.right(channel);
        } catch (final IOException e) {
            return Either.left(e);
        }
    }
}
