package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.SimplePath;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FunctionalInterface
public interface FSContext {

    FileSystem fileSystem();

    @Deprecated
    default String homeDirectory() {
        return fileSystem().getPath(System.getProperty("user.home")).toString();
    }

    default SimplePath homeDir() {
        return SimplePath.of(fileSystem().getPath(System.getProperty("user.home")));
    }

    @Deprecated
    default String workingDirectory() {
        return fileSystem().getPath(System.getProperty("user.dir")).toString();
    }

    default SimplePath workingDir() {
        return SimplePath.of(fileSystem().getPath(System.getProperty("user.dir")));
    }

    default Path getPath(String... path) {
        String[] paths = Arrays
            .stream(path)
            .toArray(String[]::new);
        return fileSystem().getPath("", paths);
    }

    default boolean isFile(final String path) {
        Preconditions.checkNotNull(path);
        return Files.isRegularFile(fileSystem().getPath(path));
    }

    default boolean isDirectory(final String path) {
        Preconditions.checkNotNull(path);
        return Files.isDirectory(fileSystem().getPath(path));
    }

    default boolean exists(final String path) {
        Preconditions.checkNotNull(path);
        return Files.exists(fileSystem().getPath(path).toAbsolutePath());
    }

    default Optional<IOException> createDirectory(final String p) {
        Preconditions.checkNotNull(p);
        final Path path = fileSystem().getPath(p);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            return Optional.of(e);
        }
        if (!Files.isDirectory(path) || !Files.exists(path)) {
            return Optional.of(new IOException("Could not of a directory at " + path));
        }
        return Optional.empty();
    }

    default Either<IOException, String> readFile(final String path) {
        Preconditions.checkNotNull(path);
        final Path q = fileSystem().getPath(path);
        try {
            final String content = Files.readAllLines(fileSystem().getPath(q.toString()), Charset.defaultCharset())
                .stream()
                .collect(Collectors.joining("\n"));
            return Either.right(content);
        } catch (final IOException e) {
            return Either.left(e);
        }
    }

    default Optional<IOException> writeFile(final SimplePath p, final String content, final boolean overwrite) {
        Preconditions.checkNotNull(p);
        Preconditions.checkNotNull(content);
        final Path path = p.toPath(fileSystem());
        try {
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
            Files.write(path, ImmutableList.of(content), Charset.defaultCharset(), StandardOpenOption.CREATE);
            return Optional.empty();
        } catch (final IOException e) {
            return Optional.of(e);
        }
    }

    default Optional<IOException> writeFile(final SimplePath path, final String content) {
        return writeFile(path, content, false);
    }

        @Deprecated
    default Optional<IOException> writeFile(final String p, final String content, final boolean overwrite) {
        Preconditions.checkNotNull(p);
        Preconditions.checkNotNull(content);
        final Path path = fileSystem().getPath(p);
        try {
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
            Files.write(path, ImmutableList.of(content), Charset.defaultCharset(), StandardOpenOption.CREATE);
            return Optional.empty();
        } catch (final IOException e) {
            return Optional.of(e);
        }
    }

    default Optional<IOException> touch(final String p) {
        Preconditions.checkNotNull(p);
        final Path path = fileSystem().getPath(p);
        try {
            if (!Files.exists(path)) {
                if (path.getParent() != null && !Files.exists(path.getParent())) {
                    Files.createDirectories(path.getParent());
                }
                Files.createFile(path);
            }
            return Optional.empty();
        } catch (final IOException e) {
            return Optional.of(e);
        }
    }

    default Optional<IOException> deleteFile(final String path) {
       final Path p = fileSystem().getPath(path);
       try {
           Files.deleteIfExists(p);
           return Optional.empty();
       } catch (final IOException e) {
           return Optional.of(e);
       }
    }

    default Optional<IOException> writeFile(final String path, final String content) {
        return writeFile(path, content, false);
    }

    default Either<IOException, ImmutableList<String>> listFiles(final String p) {
        Preconditions.checkNotNull(p);
        final Path path = fileSystem().getPath(p);
        try (final Stream<Path> paths = java.nio.file.Files.list(path)) {
            return Either.right(paths
                .map(Path::toString)
                .collect(ImmutableList.toImmutableList()));
        } catch (final IOException e) {
            return Either.left(e);
        }
    }

    static FSContext actual() {
        return of(FileSystems.getDefault());
    }

    static FSContext fake() {
        return of(Jimfs.newFileSystem(Configuration.unix()));
    }

    static FSContext of(final FileSystem fs) {
        Preconditions.checkNotNull(fs);
        return () -> fs;
    }
}
