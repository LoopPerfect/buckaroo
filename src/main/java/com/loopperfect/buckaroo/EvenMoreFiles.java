package com.loopperfect.buckaroo;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.MoreFiles;
import com.google.common.jimfs.Jimfs;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

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
            Files.deleteIfExists(path);
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

    public static HashCode hashFile(final Path path) throws IOException {

        Preconditions.checkNotNull(path);

        final HashFunction hashFunction = Hashing.sha256();
        final HashCode hashCode = hashFunction.newHasher()
            .putBytes(MoreFiles.asByteSource(path).read())
            .hash();

        return hashCode;
    }

    public static synchronized FileSystem zipFileSystem(final Path pathToZipFile) throws IOException {
        Preconditions.checkNotNull(pathToZipFile);
        try {
            return FileSystems.getFileSystem(pathToZipFile.toUri());
        } catch (Exception e) {
            try {
                return FileSystems.getFileSystem(URI.create("jar:" + pathToZipFile.toUri()));
            } catch (Exception e2) {
                return FileSystems.newFileSystem(URI.create("jar:" + pathToZipFile.toUri()), new HashMap<>());
            }
        }
    }

    /*
     * Unzips a zip file at the given path into the given target path.
     * Optionally, a sub-path can be supplied which can be used to change the "root" of the zip file.
     *
     * For example:
     *
     *  source: /User/Bob/myZip.zip
     *  target: /User/Bob/myZip
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
    public static void unzip(
        final Path source, final Path target, final Optional<Path> subPath, CopyOption... copyOptions) throws IOException {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(subPath);
        Preconditions.checkNotNull(copyOptions);
        Preconditions.checkArgument(!subPath.isPresent() || subPath.get().isAbsolute());

        try (final FileSystem zipFileSystem = zipFileSystem(source)) {
            EvenMoreFiles.copyDirectory(
                subPath.map(x -> switchFileSystem(zipFileSystem, x))
                    .orElse(zipFileSystem.getPath(zipFileSystem.getSeparator())).toAbsolutePath(),
                target,
                copyOptions);
        }
    }

    public static Stream<Path> walkZip(final Path source, final int maxDepth) throws IOException {
        Preconditions.checkNotNull(source);
        final FileSystem zipFileSystem = zipFileSystem(source);
        return Files.walk(zipFileSystem.getPath("/"), maxDepth);
    }

    public static String read(final Path path) throws IOException {
        return com.google.common.io.MoreFiles.asCharSource(path, Charsets.UTF_8).read();
    }

    public static void copyDirectory(final Path source, final Path target, final CopyOption... copyOptions) throws IOException {
        final CopyFileVisitor visitor = new CopyFileVisitor(source, target, copyOptions);
        Files.walkFileTree(source, visitor);
        if (visitor.error.isPresent()) {
            throw visitor.error.get();
        }
    }

    private static final class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private final Path source;
        private final Path target;
        private final CopyOption[] copyOptions;

        public Optional<IOException> error;

        public CopyFileVisitor(final Path source, final Path target, final CopyOption... copyOptions) {

            this.source = source;
            this.target = target;
            this.copyOptions = copyOptions;

            this.error = Optional.empty();
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes) {
            try {
                Path targetFile = target.resolve(switchFileSystem(
                    target.getFileSystem(), source.relativize(file)));
                Files.copy(file, targetFile, copyOptions);
                return FileVisitResult.CONTINUE;
            } catch (final IOException exception) {
                this.error = Optional.of(exception);
                return FileVisitResult.TERMINATE;
            }
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path directory, final BasicFileAttributes attributes) {
            try {
                final Path nextDirectory = target.resolve(
                    switchFileSystem(target.getFileSystem(), source.relativize(directory)));
                Files.createDirectories(nextDirectory);
                return FileVisitResult.CONTINUE;
            } catch (final IOException exception) {
                this.error = Optional.of(exception);
                return FileVisitResult.TERMINATE;
            }
        }
    }

    public static Path switchFileSystem(final FileSystem fs, final Path path) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(path);

        return Streams.stream(path).reduce(
            fs.getPath(path.isAbsolute() ? fs.getSeparator() : ""),
            (state, next) -> state.resolve(next.getFileName().toString()));
    }
}
