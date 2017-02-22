package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.loopperfect.buckaroo.BuckarooConfig;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RemoteCookBook;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class BuckarooConfigSerializerTest {

    @Test
    public void testBuckarooConfigSerializer() {
        final BuckarooConfig config = BuckarooConfig.of(ImmutableList.of(
            RemoteCookBook.of(
                Identifier.of("cookbook"),
                "git@github.com:njlr/buckaroo-recipes-test.git")));
        final Gson gson = Serializers.gson();
        final String serializedConfig = gson.toJson(config);
        final BuckarooConfig deserializedConfig = gson.fromJson(serializedConfig, BuckarooConfig.class);
        assertEquals(config, deserializedConfig);
    }
}
