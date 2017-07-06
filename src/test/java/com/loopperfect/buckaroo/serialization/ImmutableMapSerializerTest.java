package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ImmutableMapSerializerTest {

    @Test
    public void test() {

        final ImmutableMap<String, String> a = ImmutableMap.of(
            "A", "x",
            "B", "y",
            "C", "z");

        final String serialized = Serializers.gson.toJson(a);
        final ImmutableMap<String, String> deserialized = Serializers.gson.fromJson(
            serialized,
            TypeToken.getParameterized(ImmutableMap.class, String.class, String.class).getType());

        assertEquals(a, deserialized);
    }
}
