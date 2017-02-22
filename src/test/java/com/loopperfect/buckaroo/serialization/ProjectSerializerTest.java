package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ProjectSerializerTest {

    @org.junit.Test
    public void testProjectSerializer1() throws Exception {

        final Project project = Project.of(
                Identifier.of("my-magic-tool"),
                Optional.of("MIT"),
                DependencyGroup.of(ImmutableMap.of(
                        Identifier.of("my-magic-lib"),
                        ExactSemanticVersion.of(SemanticVersion.of(4, 5, 6)),
                        Identifier.of("some-other-lib"),
                        ExactSemanticVersion.of(
                                SemanticVersion.of(4, 1),
                                SemanticVersion.of(4, 2)),
                        Identifier.of("awesome-lib"),
                        AnySemanticVersion.of())));

        final Gson gson = Serializers.gson(true);

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

    @org.junit.Test
    public void testProjectSerializer3() throws Exception {
        try {
            final Project project = Serializers.gson().fromJson("", Project.class);
            assertTrue(false);
        } catch (final JsonParseException e) {
            assertTrue(true);
        }
    }
}
