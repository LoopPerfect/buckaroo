package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.serialization.Serializers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Recipes {

    public static Optional<ImmutableSet<Recipe>> find(final Path path) {

        Preconditions.checkNotNull(path);

        if (!Files.exists(path) && !Files.isDirectory(path)) {
            return Optional.empty();
        }

        final Gson gson = Serializers.gson();

        try (final Stream<Path> paths = Files.walk(path, 1, FileVisitOption.FOLLOW_LINKS)) {
             final List<Recipe> recipes = paths.map(x -> x.toFile())
                    .filter(x -> x.exists() &&
                            x.canRead() &&
                            x.isFile() &&
                            com.google.common.io.Files.getFileExtension(x.getPath()).equalsIgnoreCase("json"))
                    .map(x -> {
                        try {
                            return com.google.common.io.Files.asCharSource(x, Charset.defaultCharset()).read();
                        } catch (final IOException e) {
                            return null;
                        }
                    })
                     .filter(x -> x != null)
                     .map(x -> {
                        try {
                            return Optional.of(gson.fromJson(x, Recipe.class));
                        } catch (final JsonParseException e) {
                            return Optional.empty();
                        }
                    })
                     .filter(x -> x.isPresent())
                     .map(x -> (Recipe) x.get())
                    .collect(Collectors.toList());
            return Optional.of(ImmutableSet.copyOf(recipes));
        } catch (final IOException e) {
            return Optional.empty();
        }
    }
}
