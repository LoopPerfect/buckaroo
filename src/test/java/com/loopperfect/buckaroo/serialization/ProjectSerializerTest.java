package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ProjectSerializerTest {

    @org.junit.Test
    public void testProjectSerializer1() throws Exception {

        final Project project = new Project(
                Identifier.of("my-magic-tool"),
                Optional.of("MIT"),
                ImmutableSet.of(
                        Dependency.of(
                                Identifier.of("my-magic-lib"),
                                ExactSemanticVersion.of(SemanticVersion.of(4, 5, 6))),
                        Dependency.of(
                                Identifier.of("some-other-lib"),
                                ExactSemanticVersion.of(SemanticVersion.of(4, 1)))));

        final Gson gson = Serializers.gson();

        final String serializedProject = gson.toJson(project);

        final Project deserializedProject = gson.fromJson(serializedProject, Project.class);

        assertEquals(project, deserializedProject);
    }

    @org.junit.Test
    public void testProjectSerializer2() throws Exception {
        try {
            Serializers.gson().fromJson("this is not valid", Project.class);
            assertTrue(false);
        } catch (final JsonParseException e) {
            assertTrue(true);
        }
    }
}
