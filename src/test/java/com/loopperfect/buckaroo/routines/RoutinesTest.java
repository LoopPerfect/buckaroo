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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOError;
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

        IOContext io = IOContext.fake();

        Path path = io.fs().getPath(
            io.fs().getUserHomeDirectory().toString(),
            ".buckaroo",
            "config.json"
        );

        io.fs().createDirectory(path.getParent());

        assertTrue(
            Routines
                .loadConfig
                .run(io)
                .join(x->x, x->null) instanceof java.nio.file.NoSuchFileException

        );
    }

    @Test
    public void loadConfigParsingInvalidString() throws Exception {

        IOContext io = IOContext.fake();

        Path path = io.fs().getPath(
            io.fs().getUserHomeDirectory().toString(),
            ".buckaroo",
            "config.json"
        );

        io.fs().createDirectory(path.getParent());
        io.fs().writeFile(path, "");

        assertTrue(
            Routines
                .loadConfig
                .run(io)
                .join(x -> x, x -> null) != null

        );
    }

    @Test
    public void loadConfigParsesEmptsCoockbook() throws Exception {

        IOContext io = IOContext.fake();
        final Path configPath = io.fs().getPath(
            io.fs().getUserHomeDirectory().toString(),
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