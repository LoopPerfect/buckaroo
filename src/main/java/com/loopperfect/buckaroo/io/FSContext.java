package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Either;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    default Path userHomeDirectory() {
        return getFS().getPath(System.getProperty("user.home"));
    }

    default Path workingDirectory() {
        return getFS().getPath(System.getProperty("user.dir"));
    }

    default Path getPath(String... path) {
        String[] paths = Arrays
            .stream(path)
            .toArray(size -> new String[size]);
        return getFS().getPath("", paths);
    }

    default boolean isFile(final Path path) {
        return Files.isRegularFile(path);
    }

    default boolean exists(final Path path) {
        Preconditions.checkNotNull(path);
        return Files.exists(path);
    }

    default Optional<IOException> createDirectory(final Path p) {
        Preconditions.checkNotNull(p);
        final Path path = getFS().getPath(p.toString());
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            return Optional.of(e);
        }
        if (!Files.isDirectory(path) || !Files.exists(path)) {
            return Optional.of(new IOException("Could not create a directory at " + path));
        }
        return Optional.empty();
    }

    default Either<IOException, String> readFile(final Path p) {
        Preconditions.checkNotNull(p);
        final Path path = getFS().getPath(p.toString());
        try {
            final String content = Files.readAllLines(getFS().getPath(path.toString()), Charset.defaultCharset())
                .stream()
                .collect(Collectors.joining("\n"));
            return Either.right(content);
        } catch (final IOException e) {
            return Either.left(e);
        }
    }

    default Optional<IOException> writeFile(final Path p, final String content, final boolean overwrite) {
        Preconditions.checkNotNull(p);
        Preconditions.checkNotNull(content);
        final Path path = getFS().getPath(p.toString());
        try {
            if (!overwrite && Files.exists(path)) {
                throw new IOException("There is already a file at " + path);
            }
            Files.write(path, ImmutableList.of(content), Charset.defaultCharset());
            return Optional.empty();
        } catch (final IOException e) {
            return Optional.of(e);
        }
    }

    default Optional<IOException> writeFile(final Path path, final String content) {
        return writeFile(path, content, false);
    }

    default Either<IOException, ImmutableList<Path>> listFiles(final Path p) {
        Preconditions.checkNotNull(p);
        final Path path = getFS().getPath(p.toString());
        try (Stream<Path> paths = java.nio.file.Files.walk(path, 1, FileVisitOption.FOLLOW_LINKS)) {
            return Either.right(ImmutableList.copyOf(paths.collect(Collectors.toList())));
        } catch (final IOException e) {
            return Either.left(e);
        }
    }


    static FSContext actual() {
        return create(
            FileSystems.getDefault(),
            System.getProperty("user.home"),
            System.getProperty("user.dir"));
    }

    static FSContext fake() {
        return create(
            Jimfs.newFileSystem(Configuration.unix()),
            System.getProperty("user.home"),
            System.getProperty("user.dir"));
    }

    static FSContext create(final FileSystem fs, final String homeDir, final String workingDir) {
        return new FSContext() {
            @Override
            public FileSystem getFS() {
                return fs;
            }

            @Override
            public Path userHomeDirectory() {
                return getFS()
                    .getPath(homeDir);
            }

            @Override
            public Path workingDirectory() {
                return getFS()
                    .getPath(workingDir);
            }
        };
    }
}