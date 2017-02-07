package com.loopperfect.buckaroo.serialization;

import com.google.gson.Gson;
import com.loopperfect.buckaroo.Dependency;
import com.loopperfect.buckaroo.ExactSemanticVersion;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.SemanticVersion;

import static org.junit.Assert.assertEquals;

public final class DependencySerializerTest {

    @org.junit.Test
    public void testDependencySerializer() {

        final Dependency dependency = Dependency.of(
                Identifier.of("magic-libs"),
                ExactSemanticVersion.of(SemanticVersion.of(3, 4)));

        final Gson gson = Serializers.gson();

        final String serializedDependency = gson.toJson(dependency);

        final Dependency deserizializedDependency = gson.fromJson(serializedDependency, Dependency.class);

        assertEquals(dependency, deserizializedDependency);
    }
}
