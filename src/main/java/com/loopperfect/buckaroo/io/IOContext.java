package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Either;

import java.io.BufferedReader;
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
                return Files.isRegularFile(path);
            }

            @Override
            public boolean exists(final Path path) {
                Preconditions.checkNotNull(path);
                return Files.exists(path);
            }

            @Override
            public Optional<IOException> createDirectory(final Path path)  {
                Preconditions.checkNotNull(path);
                try {
                    Files.createDirectories(path);
                } catch(IOException e) {
                    return Optional.of(e);
                }
                if (!Files.isDirectory(path) || !Files.exists(path)) {
                    return Optional.of(new IOException("Could not create a directory at " + path));
                }
                return Optional.empty();
            }

            @Override
            public Either<IOException, String> readFile(final Path path) {
                Preconditions.checkNotNull(path);
                try {
                    final String content = Files.readAllLines(fs.getPath(path.toString()), Charset.defaultCharset())
                        .stream()
                        .collect(Collectors.joining("\n"));
                    return Either.right(content);
                } catch (final IOException e) {
                    return Either.left(e);
                }
            }

            @Override
            public Optional<IOException> writeFile(final Path p, final String content, final boolean overwrite) {
                Preconditions.checkNotNull(p);
                Preconditions.checkNotNull(content);
                final Path path = fs.getPath(p.toString());
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

            @Override
            public Optional<IOException> writeFile(final Path path, final String content) {
                return writeFile(path, content, false);
            }

            @Override
            public Either<IOException, ImmutableList<Path>> listFiles(final Path path) {
                Preconditions.checkNotNull(path);
                try (Stream<Path> paths = java.nio.file.Files.walk(path, 1, FileVisitOption.FOLLOW_LINKS)) {
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
