package com.loopperfect.buckaroo.routines;


import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.io.IOContext;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Created by gaetano on 20/02/17.
 */
public final class RoutinesTest {

    @Test
    public void loadConfigReadFileFailed() throws Exception {

        IOContext io = IOContext.fake();

        Path path = io.fs().getPath(
            io.fs().userHomeDirectory().toString(),
            ".buckaroo",
            "config.json"
        );

        io.fs().createDirectory(path.getParent().toString());

        assertTrue(Routines.readConfig(path.toString()).run(io)
            .join(x -> x, x -> null) instanceof java.nio.file.NoSuchFileException);
    }

    @Test
    public void loadConfigParsingInvalidString() throws Exception {

        final IOContext io = IOContext.fake();

        final Path path = io.fs().getPath(
                io.fs().userHomeDirectory(),
                ".buckaroo",
                "config.json");

        io.fs().createDirectory(path.getParent().toString());
        io.fs().writeFile(path.toString(), "");

        assertTrue(Routines.readConfig(path.toString()).run(io).join(x -> true, x -> false));
    }

    @Test
    public void loadConfigParsesEmptsCookbook() throws Exception {

        final IOContext io = IOContext.fake();

        final Path path = io.fs().getPath(
                io.fs().userHomeDirectory(),
                ".buckaroo/",
                "config.json");

        final String content = "{" +
            "\"cookBooks\":[]" +
            "}";

        Files.createDirectories(path.getParent());
        Files.write(path, ImmutableList.of(content), Charset.defaultCharset());

        System.out.println(path.toString());

        assertEquals(Routines.readConfig(path.toString()).run(io).rightProjection(x -> x.cookBooks),
            Either.right(ImmutableList.of()));
    }

}
