package com.loopperfect.buckaroo.buck;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.ini4j.Ini;
import org.ini4j.Profile;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.stream.Stream;

public final class BuckConfig {

    private BuckConfig() {

    }

    private final static String repositoriesSection = "repositories";

    public static ImmutableMap<String, ImmutableMap<String, String>> parse(final String s) throws Exception {
        final Ini ini = new Ini();

        final Reader reader = new StringReader(s);

        ini.load(reader);

        final ImmutableMap.Builder<String, ImmutableMap<String, String>> builder = ImmutableMap.builder();

        for (final Map.Entry<String, Profile.Section> i : ini.entrySet()) {
            final ImmutableMap.Builder<String, String> sectionBuilder = ImmutableMap.builder();

            for (final Map.Entry<String, String> j : i.getValue().entrySet()) {
                sectionBuilder.put(j);
            }

            builder.put(i.getKey(), sectionBuilder.build());
        }

        return builder.build();
    }

    public static ImmutableMap<String, ImmutableMap<String, String>> removeBuckarooConfig(final ImmutableMap<String, ImmutableMap<String, String>> config) {
        final ImmutableMap.Builder<String, ImmutableMap<String, String>> builder = ImmutableMap.builder();

        for (final Map.Entry<String, ImmutableMap<String, String>> i : config.entrySet()) {
            if (i.getKey().equals(repositoriesSection)) {
                final ImmutableMap<String, String> section = i.getValue();
                final ImmutableMap.Builder<String, String> sectionBuilder = ImmutableMap.builder();

                for (final Map.Entry<String, String> j : section.entrySet()) {
                    final Boolean isBuckarooEntry = j.getValue().contains("buckaroo");

                    if (!isBuckarooEntry) {
                        sectionBuilder.put(j);
                    }
                }

                builder.put(repositoriesSection, sectionBuilder.build());
            } else {
                builder.put(i);
            }
        }

        return builder.build();
    }

    public static ImmutableMap<String, ImmutableMap<String, String>> override(final ImmutableMap<String, ImmutableMap<String, String>> base, ImmutableMap<String, ImmutableMap<String, String>> override) {
        final ImmutableMap.Builder<String, ImmutableMap<String, String>> builder = ImmutableMap.builder();

        final ImmutableSet<String> sections = Stream.concat(base.keySet().stream(), override.keySet().stream())
                .collect(ImmutableSet.toImmutableSet());

        for (final String key : sections) {
            final ImmutableMap<String, String> section = Stream.concat(
                    base.getOrDefault(key, ImmutableMap.of()).entrySet().stream(),
                    override.getOrDefault(key, ImmutableMap.of()).entrySet().stream())
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, (i, j) -> j));

            builder.put(key, section);
        }

        return builder.build();
    }
}
