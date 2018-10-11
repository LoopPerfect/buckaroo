package com.loopperfect.buckaroo.buck;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class BuckConfigTest {

    @Test
    public void merge() throws Exception {

        final ImmutableMap<String, ImmutableMap<String, String>> a = ImmutableMap.of(
                "x",
                ImmutableMap.of(
                        "a", "hello"
                ),
                "y",
                ImmutableMap.of("a", "123", "b", "456")
        );

        final ImmutableMap<String, ImmutableMap<String, String>> b = ImmutableMap.of(
                "y",
                ImmutableMap.of("a", "789", "c", "246"),
                "z",
                ImmutableMap.of(
                        "a", "hello"
                )
        );

        final ImmutableMap<String, ImmutableMap<String, String>> actual = BuckConfig.override(a, b);

        final ImmutableMap<String, ImmutableMap<String, String>> expected = ImmutableMap.of(
                "x",
                ImmutableMap.of("a", "hello"),

                "y",
                ImmutableMap.of("a", "789", "b", "456", "c", "246"),
                "z",
                ImmutableMap.of(
                        "a", "hello"
                )
        );

        assertEquals(expected, actual);
    }

    @Test
    public void removeBuckarooConfig() throws Exception {

        final ImmutableMap<String, ImmutableMap<String, String>> config = BuckConfig.parse("[project]\n" +
            "  ignore = .git, .idea, out\n" +
            "\n" +
            "[build]\n" +
            "  engine = deep\n" +
            "\n" +
            "\n" +
            "[repositories]\n" +
            "  custom = ./third-party/custom\n" +
            "  boost-config = ./buckaroo/official/boost/config\n");

        final ImmutableMap<String, ImmutableMap<String, String>> actual = BuckConfig.removeBuckarooConfig(config);

        final ImmutableMap<String, ImmutableMap<String, String>> expected = ImmutableMap.of(
                "project",
                ImmutableMap.of(
                        "ignore", ".git, .idea, out"
                ),
                "build",
                ImmutableMap.of(
                        "engine",
                        "deep"
                ),
                "repositories",
                ImmutableMap.of(
                        "custom",
                        "./third-party/custom"
                )
        );

        assertEquals(expected, actual);
    }

    @Test
    public void serialize() {
        final ImmutableMap<String, ImmutableMap<String, String>> config = ImmutableMap.of(
                "repositories",
                ImmutableMap.of(
                        "custom",
                        "./third-party/custom"
                )
        );

        final String actual = BuckConfig.serialize(config);

        final String expected = "[repositories]\n" +
                "  custom = ./third-party/custom\n" +
                "\n";

        assertEquals(expected, actual);
    }
}
