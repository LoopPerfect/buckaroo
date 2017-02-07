package com.loopperfect.buckaroo;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public final class ProjectTest {

    @org.junit.Test
    public void serialization() throws Exception {

        final Project project = new Project(
                Identifier.of("my-magic-tool"),
                Optional.of("MIT"),
                ImmutableSet.of(
                        Dependency.of(
                                Identifier.of("my-magic-lib"),
                                AnySemanticVersion.of()),
                        Dependency.of(
                                Identifier.of("some-other-lib"),
                                ExactSemanticVersion.of(SemanticVersion.of(4, 1)))));

        final Gson gson = new Gson();

        final String serializedProject = gson.toJson(project);

        final Project deserializedProject = gson.fromJson(serializedProject, Project.class);

        assertEquals(project, deserializedProject);
    }
}
