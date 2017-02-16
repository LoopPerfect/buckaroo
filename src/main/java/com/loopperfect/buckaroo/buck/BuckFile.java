package com.loopperfect.buckaroo.buck;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Project;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

public final class BuckFile {

    private BuckFile() {

    }

    public static Either<IOException, String> generate(final Project project) {

        Preconditions.checkNotNull(project);

        final URL url = Resources.getResource("com.loopperfect.buckaroo/ProjectTemplate.mustache");
        final String templateString;
        try {
            templateString = Resources.toString(url, Charsets.UTF_8);
        } catch (final IOException e) {
            return Either.left(e);
        }

        final Map<String, Object> scopes = ImmutableMap.of(
                "name", project.name.name,
                "dependencies", project.dependencies.keySet()
                        .stream()
                        .collect(Collectors.toList()));

        final Writer writer = new StringWriter();
        final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        final Mustache mustache = mustacheFactory.compile(new StringReader(templateString), "Project");

        mustache.execute(writer, scopes);

        try {
            writer.flush();
        } catch (final IOException e) {
            return Either.left(e);
        }

        return Either.right(writer.toString());
    }
}
