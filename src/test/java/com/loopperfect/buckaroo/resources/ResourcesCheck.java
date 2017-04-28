package com.loopperfect.buckaroo.resources;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;

public final class ResourcesCheck {

    @Test
    public void defaultConfigExists() throws IOException {
        final String defaultConfig = Resources.toString(
            Resources.getResource("com.loopperfect.buckaroo/DefaultConfig.txt"),
            Charsets.UTF_8);
        assert(defaultConfig.length() > 0);
    }
}
