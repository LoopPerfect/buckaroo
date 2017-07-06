package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import com.loopperfect.buckaroo.versioning.ExactSemanticVersion;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ProjectSerializerTest {

    @org.junit.Test
    public void testProjectSerializer1() throws Exception {

        final Project project = Project.of(
            Optional.of("my-magic-tool"),
            Optional.of("my-magic-tool-lib"),
            Optional.of("MIT"),
            DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "my-magic-lib"),
                ExactSemanticVersion.of(SemanticVersion.of(4, 5, 6)),
                RecipeIdentifier.of("org", "some-other-lib"),
                ExactSemanticVersion.of(
                    SemanticVersion.of(4, 1),
                    SemanticVersion.of(4, 2)),
                RecipeIdentifier.of("org", "awesome-lib"),
                AnySemanticVersion.of())));

        final String serializedProject = Serializers.serialize(project);

        final Either<JsonParseException, Project> deserializedProject =
                Serializers.parseProject(serializedProject);

        assertEquals(Either.right(project), deserializedProject);
    }

    @org.junit.Test
    public void testProjectSerializer2() throws Exception {
        assertTrue(Serializers.parseProject("this is not valid").left().isPresent());
    }

    @org.junit.Test
    public void testProjectSerializer3() throws Exception {
        assertTrue(Serializers.parseProject("").left().isPresent());
    }

    @org.junit.Test
    public void testProjectSerializer4() throws Exception {

        final Project project = Project.of();

        final String serializedProject = Serializers.serialize(project);

        final Either<JsonParseException, Project> deserializedProject =
            Serializers.parseProject(serializedProject);

        assertEquals(Either.right(project), deserializedProject);
    }
}
