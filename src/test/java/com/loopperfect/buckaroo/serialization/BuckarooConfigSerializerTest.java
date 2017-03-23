package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.loopperfect.buckaroo.BuckarooConfig;
import com.loopperfect.buckaroo.Either;
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
                        "git@github.com:njlr/buckaroo-organizations-test.git")));
        final String serializedConfig = Serializers.serialize(config);
        final Either<JsonParseException, BuckarooConfig> deserializedConfig =
                Serializers.parseConfig(serializedConfig);
        assertEquals(Either.right(config), deserializedConfig);
    }
}
