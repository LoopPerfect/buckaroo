package com.loopperfect.buckaroo.tasks;

import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.GitCommitHash;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public final class GitTasksTest {

    @Test
    public void fetchTags1() throws Exception {

        final ImmutableMap<String, GitCommitHash> expected = ImmutableMap.of(
            "v2", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"),
            "v0.1.0", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"),
            "v1.0.0-rc1", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"));

        final ImmutableMap<String, GitCommitHash> actual = GitTasks.fetchTags(
            "git@github.com:njlr/test-lib-tags.git")
            .timeout(5000L, TimeUnit.MILLISECONDS)
            .blockingGet();

        assertEquals(expected, actual);
    }

    @Test
    public void fetchTags2() throws Exception {

        final ImmutableMap<String, GitCommitHash> expected = ImmutableMap.of(
            "v2", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"),
            "v0.1.0", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"),
            "v1.0.0-rc1", GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b"));

        final ImmutableMap<String, GitCommitHash> actual = GitTasks.fetchTags(
            "https://github.com/njlr/test-lib-tags.git")
            .timeout(5000L, TimeUnit.MILLISECONDS)
            .blockingGet();

        assertEquals(expected, actual);
    }

    @Test
    public void fetchTags3() throws Exception {

        final ImmutableMap<String, GitCommitHash> expected = ImmutableMap.of(
            "v0.1.0", GitCommitHash.of("4ead5911c2d85e06873eecd0dff6229ada3e101c"));

        final ImmutableMap<String, GitCommitHash> actual = GitTasks.fetchTags(
            "git@bitbucket.org:njlr/hello-buckaroo.git")
            .timeout(5000L, TimeUnit.MILLISECONDS)
            .blockingGet();

        assertEquals(expected, actual);
    }
}
