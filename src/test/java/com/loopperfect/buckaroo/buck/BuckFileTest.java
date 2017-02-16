package com.loopperfect.buckaroo.buck;

import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.*;

public class BuckFileTest {

    @Test
    public void generate() throws Exception {

        final Identifier project = Identifier.of("my-magic-tool");
        final ImmutableMap<Identifier, SemanticVersion> resolvedDependencies =
                ImmutableMap.of(
                        Identifier.of("my-magic-lib"),
                        SemanticVersion.of(4, 5, 6),
                        Identifier.of("some-other-lib"),
                        SemanticVersion.of(4, 1));

        final Either<IOException, String> generatedProject = BuckFile.generate(project, resolvedDependencies);

        assertTrue(generatedProject.join(error -> false, string -> string.length() > 3));
    }
}