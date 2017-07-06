package com.loopperfect.buckaroo;

import org.junit.Test;

import java.net.URL;
import java.util.Optional;

import static org.junit.Assert.*;

public final class URLUtilsTest {

    @Test
    public void getExtension() throws Exception {

        assertEquals(Optional.of("zip"), URLUtils.getExtension(new URL("http://www.example.com/stuff.zip")));
        assertEquals(Optional.of("zip"), URLUtils.getExtension(new URL("http://www.example.com/stuff.zip?u=1")));
        assertEquals(Optional.of("zip"), URLUtils.getExtension(new URL("http://www.example.com/a/b/c/stuff.zip")));
        assertEquals(Optional.of("zip"), URLUtils.getExtension(new URL("http://www.example.com/a/b/c/v1.0.0.zip")));

        assertEquals(Optional.empty(), URLUtils.getExtension(new URL("http://www.example.com")));
        assertEquals(Optional.empty(), URLUtils.getExtension(new URL("http://www.example.com/")));
        assertEquals(Optional.empty(), URLUtils.getExtension(new URL("http://www.example.com/.")));
    }
}