package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Either;

import java.io.*;
import java.nio.channels.ByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class Files {

    private Files() {
        super();
    }

    public static Optional<IOException> unzip(final Path source, final Path target, final Optional<Path> subPath) {
        final File targetDirectory = target.toFile();
        if (!targetDirectory.exists()) {
            targetDirectory.mkdir();
        }
        try (final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(source.toFile()))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                final ZipEntry current = entry;
                // Check that the current entry lives in the sub-path (or there is no sub-path!)
                if (subPath.map(x -> current.getName().startsWith(x.toString())).orElse(true)) {
                    final Path filePath = Paths.get(
                        targetDirectory.toPath().toString(),
                        current.getName().substring(subPath.map(x -> x.toString().length()).orElse(0)));
                    if (current.isDirectory()) {
                        File dir = filePath.toFile();
                        dir.mkdir();
                    } else {
                        extractFile(zipIn, filePath);
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        } catch (final IOException e) {
            return Optional.of(e);
        }
        return Optional.empty();
    }

    private static final int BUFFER_SIZE = 4096;

    private static void extractFile(final ZipInputStream zipIn, final Path filePath) throws IOException {
        try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
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
