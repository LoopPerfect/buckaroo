package com.loopperfect.buckaroo.routines;


import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.GitCommit;
import com.loopperfect.buckaroo.Routine;
import com.loopperfect.buckaroo.io.GitContext;
import com.loopperfect.buckaroo.io.IOContext;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;


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

        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        IOContext io = IOContext.actual(fs);
        final Path configPath = fs.getPath(
            io.getUserHomeDirectory().toString(),
            ".buckaroo/",
            "config.json");

        final String content = "{\"cookBooks:\"[]}";

        java.nio.file.Files.createDirectories(configPath.getParent());
        java.nio.file.Files.write(
            configPath,
            ImmutableList.of(content),
            StandardCharsets.UTF_8
        );


        assertEquals(
            Routines.loadConfig
                .run(io)
                .join(x->null, x->x)
                .cookBooks,
            ImmutableList.of());
    }

}