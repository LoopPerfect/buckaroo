package com.loopperfect.buckaroo.buck;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.SemanticVersion;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

public final class BuckFile {

    private BuckFile() {

    }

    public static Either<IOException, String> generate(final Identifier project, final ImmutableMap<Identifier, SemanticVersion> resolvedDependencies) {

        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(resolvedDependencies);

        final URL url = Resources.getResource("com.loopperfect.buckaroo/ProjectTemplate.mustache");
        final String templateString;
        try {
            templateString = Resources.toString(url, Charsets.UTF_8);
        } catch (final IOException e) {
            return Either.left(e);
        }

        final Map<String, Object> scopes = ImmutableMap.of(
            "name", project.name,
            "dependencies", resolvedDependencies.entrySet()
                .stream()
                .map(x -> RecipeIdentifier.of(x.getKey(), x.getValue()))
                .collect(ImmutableList.toImmutableList()));

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

    public static Either<IOException, String> list(final String name, final ImmutableList<String> values) {

        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(values);

        final URL url = Resources.getResource("com.loopperfect.buckaroo/BuckListTemplate.mustache");
        final String templateString;
        try {
            templateString = Resources.toString(url, Charsets.UTF_8);
        } catch (final IOException e) {
            return Either.left(e);
        }

        final Map<String, Object> scopes = ImmutableMap.of(
            "name", name,
            "values", values);

        final Writer writer = new StringWriter();
        final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
        final Mustache mustache = mustacheFactory.compile(new StringReader(templateString), "BuckList");

        mustache.execute(writer, scopes);

        try {
            writer.flush();
        } catch (final IOException e) {
            return Either.left(e);
        }

        return Either.right(writer.toString());
    }
}
