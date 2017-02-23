package com.loopperfect.buckaroo.buck;

import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Identifier;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class BuckFileTest {

    @Test
    public void generate() throws Exception {

        final Identifier project = Identifier.of("my-magic-tool");
        final Either<IOException, String> generatedProject = BuckFile.generate(project);

        assertTrue(generatedProject.join(error -> false, string -> string.length() > 3));
    }

    @Test
    public void list() {

        final Either<IOException, String> generatedList = BuckFile.list(
                "buckarooDeps",
                ImmutableList.of(
                        "//buckaroo/awesome/1.0.0:awesome",
                        "//buckaroo/some-lib/2.0.1:some-lib"));

        assertTrue(generatedList.join(error -> false, string -> string.length() > 10));
    }
}