package com.loopperfect.buckaroo;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public final class GitCommitHashTest {

    @Test
    public void parse() throws Exception {

        assertEquals(
            Optional.of(GitCommitHash.of("138252fac310b976a5ee55ffaa8e9180cf44112b")),
            GitCommitHash.parse("138252fac310b976a5ee55ffaa8e9180cf44112b"));

        assertEquals(
            Optional.of(GitCommitHash.of("717c2f9")),
            GitCommitHash.parse("717c2f9"));

        assertEquals(
            Optional.empty(),
            GitCommitHash.parse("abc"));

        assertEquals(
            Optional.empty(),
            GitCommitHash.parse("x38252fac310b976a5ee55ffaa8e9180cf44112b"));
    }
}