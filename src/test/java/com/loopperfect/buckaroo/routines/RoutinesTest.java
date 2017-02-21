package com.loopperfect.buckaroo.routines;


import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Either;

import com.loopperfect.buckaroo.io.IOContext;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Created by gaetano on 20/02/17.
 */
public class RoutinesTest {
    @Test
    public void loadConfigReadFileFailed() throws Exception {
        IOContext io = mock(IOContext.class);
        Path p = mock(Path.class);

        IOException error = new IOException("foo");
        when(p.toString()).thenReturn("/");
        when(io.getUserHomeDirectory()).thenReturn(p);
        when(io.readFile(Mockito.any())).thenReturn(Either.left(error));

        assertEquals(
            Routines
                .loadConfig
                .run(io)
                .join(x->x, x->null),
            error
        );
    }

    @Test
    public void loadConfigParsingInvalidString() throws Exception {
        IOContext io = mock(IOContext.class);
        Path p = mock(Path.class);

        when(p.toString()).thenReturn("/");
        when(io.getUserHomeDirectory()).thenReturn(p);
        when(io.readFile(Mockito.any())).thenReturn(Either.right(""));

        //TODO: check type of IOException
        assertNotNull(
            Routines.loadConfig
                .run(io)
                .join(x->x, x->null)
        );

    }

    @Test
    public void loadConfigParsingEmptyCookbook() throws Exception {
        IOContext io = mock(IOContext.class);
        Path p = mock(Path.class);

        IOException error = new IOException("foo");
        when(p.toString()).thenReturn("/");
        when(io.getUserHomeDirectory()).thenReturn(p);
        when(io.readFile(Mockito.any())).thenReturn(
            Either.right("{" +
                "\"cookBooks\":[]" +
                "}")
        );

        assertEquals(
            Routines.loadConfig
                .run(io)
                .join(x->null, x->x)
                .cookBooks,
            ImmutableList.of());
    }

    @Test
    public void loadConfigFromFakeFs() throws Exception {

        IOContext io = IOContext.fake();
        final Path configPath = io.getPath(
            io.getUserHomeDirectory().toString(),
            ".buckaroo/",
            "config.json");

        final String content = "{" +
            "\"cookBooks\":[]" +
            "}";

        Files.createDirectories(configPath.getParent());
        Files.write(
            configPath,
            ImmutableList.of(content),
            Charset.defaultCharset()
        );

        System.out.println(configPath.toString());

        assertEquals(
            Routines.loadConfig
                .run(io)
                .join(x->null, x->x)
                .cookBooks,
            ImmutableList.of());
    }

}