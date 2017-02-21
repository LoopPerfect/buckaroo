package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Either;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IOContext {

    void println();

    void println(final String x);

    Optional<String> readln();

    Path getUserHomeDirectory();

    Path getWorkingDirectory();

    boolean isFile(final Path path);

    boolean exists(final Path path);

    Optional<IOException> createDirectory(final Path path);

    Either<IOException, String> readFile(final Path path);

    Optional<IOException> writeFile(final Path path, final String content, final boolean overwrite);

    Optional<IOException> writeFile(final Path path, final String content);

    Either<IOException, ImmutableList<Path>> listFiles(final Path path);

    GitContext git();

    static File getFile(FileSystem fs, Path path) {
        return fs.getPath(path.toString())
            .toFile();
    }

    static IOContext actual(FileSystem fs) {

        final GitContext gitContext = GitContext.actual();

        return new IOContext() {

            @Override
            public void println() {
                System.out.println();
            }

            @Override
            public void println(final String x) {
                System.out.println(x);
            }

            @Override
            public Optional<String> readln() {
                if (System.console() != null) {
                    return Optional.of(System.console().readLine());
                }
                final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    return Optional.of(reader.readLine());
                } catch (final IOException e) {
                    return Optional.empty();
                }
            }

            @Override
            public Path getUserHomeDirectory() {
                return fs.getPath(System.getProperty("user.home"));
            }

            @Override
            public Path getWorkingDirectory() {
                return fs.getPath(System.getProperty("user.dir"));
            }

            @Override
            public boolean isFile(final Path path) {
                return getFile(fs, path)
                    .isFile();
            }

            @Override
            public boolean exists(final Path path) {
                Preconditions.checkNotNull(path);
                return getFile(fs, path)
                    .exists();
            }

            @Override
            public Optional<IOException> createDirectory(final Path path) {
                Preconditions.checkNotNull(path);
                getFile(fs, path).mkdir();
                if (!getFile(fs, path).isDirectory() || !getFile(fs, path).exists()) {
                    return Optional.of(new IOException("Could not create a directory at " + path));
                }
                return Optional.empty();
            }

            @Override
            public Either<IOException, String> readFile(final Path path) {
                Preconditions.checkNotNull(path);
                try {
                    final String content = Files.asCharSource(getFile(fs, path), Charset.defaultCharset()).read();
                    return Either.right(content);
                } catch (final IOException e) {
                    return Either.left(e);
                }
            }

            @Override
            public Optional<IOException> writeFile(final Path path, final String content, final boolean overwrite) {
                Preconditions.checkNotNull(path);
                Preconditions.checkNotNull(content);
                try {
                    if (!overwrite && getFile(fs, path).exists()) {
                        throw new IOException("There is already a file at " + path);
                    }
                    Files.write(content, getFile(fs, path), Charset.defaultCharset());
                    return Optional.empty();
                } catch (final IOException e) {
                    return Optional.of(e);
                }
            }

            @Override
            public Optional<IOException> writeFile(final Path path, final String content) {
                return writeFile(path, content, false);
            }

            @Override
            public Either<IOException, ImmutableList<Path>> listFiles(final Path path) {
                Preconditions.checkNotNull(path);
                try (Stream<Path> paths = java.nio.file.Files.walk(fs.getPath(path.toString()), 1, FileVisitOption.FOLLOW_LINKS)) {
                    return Either.right(ImmutableList.copyOf(paths.collect(Collectors.toList())));
                } catch (final IOException e) {
                    return Either.left(e);
                }
            }

            @Override
            public GitContext git() {
                return gitContext;
            }
        };
    }
}
