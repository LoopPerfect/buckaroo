package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Either;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by gaetano on 21/02/17.
 */
@FunctionalInterface
public interface FSContext {

    FileSystem getFS();

    default String userHomeDirectory() {
        return getFS().getPath(System.getProperty("user.home")).toString();
    }

    default String workingDirectory() {
        return getFS().getPath(System.getProperty("user.dir")).toString();
    }

    default Path getPath(String... path) {
        String[] paths = Arrays
            .stream(path)
            .toArray(String[]::new);
        return getFS().getPath("", paths);
    }

    default boolean isFile(final String path) {
        Preconditions.checkNotNull(path);
        return Files.isRegularFile(getFS().getPath(path));
    }

    default boolean isDirectory(final String path) {
        Preconditions.checkNotNull(path);
        return Files.isDirectory(getFS().getPath(path));
    }

    default boolean exists(final String path) {
        Preconditions.checkNotNull(path);
        return Files.exists(getFS().getPath(path).toAbsolutePath());
    }

    default Optional<IOException> createDirectory(final String p) {
        Preconditions.checkNotNull(p);
        final Path path = getFS().getPath(p);
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

    default Either<IOException, String> readFile(final String p) {
        Preconditions.checkNotNull(p);
        final Path path = getFS().getPath(p);
        try {
            final String content = Files.readAllLines(getFS().getPath(path.toString()), Charset.defaultCharset())
                .stream()
                .collect(Collectors.joining("\n"));
            return Either.right(content);
        } catch (final IOException e) {
            return Either.left(e);
        }
    }

    default Optional<IOException> writeFile(final String p, final String content, final boolean overwrite) {
        Preconditions.checkNotNull(p);
        Preconditions.checkNotNull(content);
        final Path path = getFS().getPath(p);
        try {
            if (Files.exists(path)) {
                if (!overwrite) {
                    throw new IOException("There is already a file at " + path);
                }
            } else {
                if (!Files.exists(path.getParent())) {
                    Files.createDirectories(path.getParent());
                }
                Files.createFile(path);
            }
            Files.write(path, ImmutableList.of(content), Charset.defaultCharset(), StandardOpenOption.CREATE);
            return Optional.empty();
        } catch (final IOException e) {
            return Optional.of(e);
        }
    }

    default Optional<IOException> deleteFile(final String path) {
       final Path p = getFS().getPath(path);
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
        final Path path = getFS().getPath(p);
        try (Stream<Path> paths = java.nio.file.Files.list(path)) {
            return Either.right(paths.map(Path::toString).collect(ImmutableList.toImmutableList()));
        } catch (final IOException e) {
            return Either.left(e);
        }
    }

    static FSContext actual() {
        return of(
            FileSystems.getDefault(),
            System.getProperty("user.home"),
            System.getProperty("user.dir"));
    }

    static FSContext fake() {
        return of(
            Jimfs.newFileSystem(Configuration.unix()),
            System.getProperty("user.home"),
            System.getProperty("user.dir"));
    }

    static FSContext of(final FileSystem fs, final String homeDirectory, final String workingDirectory) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(homeDirectory);
        Preconditions.checkNotNull(workingDirectory);

        return new FSContext() {

            @Override
            public FileSystem getFS() {
                return fs;
            }

            @Override
            public String userHomeDirectory() {
                return getFS()
                    .getPath(homeDirectory).toString();
            }

            @Override
            public String workingDirectory() {
                return getFS()
                    .getPath(workingDirectory).toString();
            }
        };
    }
}
