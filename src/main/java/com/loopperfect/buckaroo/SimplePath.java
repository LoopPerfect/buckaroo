package com.loopperfect.buckaroo;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Deprecated
public final class SimplePath {

    private ImmutableList<String> parts;

    private SimplePath(final List<String> parts) {
        Preconditions.checkNotNull(parts);
        Preconditions.checkArgument(parts.stream().noneMatch(Objects::isNull));
        this.parts = ImmutableList.copyOf(parts);
    }

    public String name() {
        return parts.isEmpty() ?
            "" :
            parts.get(parts.size() - 1);
    }

    public SimplePath parent() {
        if (parts.isEmpty()) {
            throw new IllegalStateException("This path has no parent");
        }
        return new SimplePath(
            parts.stream().limit(parts.size() - 1).collect(ImmutableList.toImmutableList()));
    }

    public SimplePath append(final String part) {
        Preconditions.checkNotNull(part);
        return new SimplePath(
            new ImmutableList.Builder<String>()
                .addAll(parts)
                .add(part)
                .build());
    }

    public SimplePath append(final String part, final String... parts) {
        Preconditions.checkNotNull(part);
        return new SimplePath(
            new ImmutableList.Builder<String>()
                .addAll(this.parts)
                .add(part)
                .addAll(ImmutableList.copyOf(parts))
                .build());
    }

    public Path toPath(final FileSystem fileSystem) {
        Preconditions.checkNotNull(fileSystem);
        return fileSystem.getPath(
            String.join(fileSystem.getSeparator(), parts));
    }

    public boolean equals(final SimplePath other) {
        Preconditions.checkNotNull(other);
        return Objects.equals(parts, other.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parts);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof SimplePath &&
            equals((SimplePath)obj);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("parts", parts)
            .toString();
    }

    public static SimplePath of(final List<String> parts) {
        return new SimplePath(parts);
    }

    public static SimplePath of(final String... parts) {
        return new SimplePath(ImmutableList.copyOf(parts));
    }

    public static SimplePath of(final Path path) {
        Preconditions.checkNotNull(path);
        final String[] parts = path.toString().split(path.getFileSystem().getSeparator());
        return new SimplePath(ImmutableList.copyOf(parts));
    }
}
