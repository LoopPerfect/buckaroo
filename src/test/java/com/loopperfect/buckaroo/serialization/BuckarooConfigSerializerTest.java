package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.BuckarooConfig;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RemoteCookbook;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public final class BuckarooConfigSerializerTest {

    @Test
    public void testBuckarooConfigSerializer1() {
        final BuckarooConfig config = BuckarooConfig.of(ImmutableList.of(
                RemoteCookbook.of(
                        Identifier.of("cookbook"),
                        "git@github.com:njlr/buckaroo-organizations-test.git")));
        final String serializedConfig = Serializers.serialize(config);
        final Either<JsonParseException, BuckarooConfig> deserializedConfig =
                Serializers.parseConfig(serializedConfig);
        assertEquals(Either.right(config), deserializedConfig);
    }

    @Test
    public void testBuckarooConfigSerializer2() throws MalformedURLException {
        final BuckarooConfig config = BuckarooConfig.of(ImmutableList.of(
            RemoteCookbook.of(
                Identifier.of("cookbook"),
                "git@github.com:njlr/buckaroo-organizations-test.git")),
            new URL("http://localhost:4444/"));
        final String serializedConfig = Serializers.serialize(config);
        final Either<JsonParseException, BuckarooConfig> deserializedConfig =
            Serializers.parseConfig(serializedConfig);
        assertEquals(Either.right(config), deserializedConfig);
    }
}
