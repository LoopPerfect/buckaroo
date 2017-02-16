package com.loopperfect.buckaroo.buck;

import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class BuckFileTest {

    @Test
    public void generate() throws Exception {

        final Project project = Project.of(
                Identifier.of("my-magic-tool"),
                Optional.of("MIT"),
                ImmutableMap.of(
                        Identifier.of("my-magic-lib"),
                        ExactSemanticVersion.of(SemanticVersion.of(4, 5, 6)),
                        Identifier.of("some-other-lib"),
                        ExactSemanticVersion.of(
                                SemanticVersion.of(4, 1),
                                SemanticVersion.of(4, 2)),
                        Identifier.of("awesome-lib"),
                        AnySemanticVersion.of()));

        assertTrue(BuckFile.generate(project).join(error -> false, string -> string.length() > 3));
    }
}