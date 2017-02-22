package com.loopperfect.buckaroo.routines;


import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.io.ConsoleContext;
import com.loopperfect.buckaroo.io.FSContext;
import com.loopperfect.buckaroo.io.GitContext;
import com.loopperfect.buckaroo.io.IOContext;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;


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
                .join(x -> x, x -> null) instanceof java.nio.file.NoSuchFileException

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
    public void loadConfigParsesEmptyCoockbook() throws Exception {

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
                .join(x -> null, x -> x)
                .cookBooks,
            ImmutableList.of());
    }



    @Test
    public void listCookBooks() throws Exception {
        List<String> output = new ArrayList<String>();

        IOContext io = IOContext.of(
            FSContext.fake(),
            GitContext.fake(),
            ConsoleContext.of(x-> {
                output.add(x);
                return null;
            }, Optional::empty)
        );

        final Path configPath = io.fs().getPath(
            io.fs().getUserHomeDirectory().toString(),
            ".buckaroo/",
            "config.json");

        final String content = "{" +
            "\"cookbooks\":[{" +
            "\"name\":\"foo\"" +
            "\"url\":\"git://foo.com\"" +
            "}]" +
            "}";

        Files.createDirectories(configPath.getParent());
        Files.write(
            configPath,
            ImmutableList.of(content),
            Charset.defaultCharset()
        );

        Routines.listCookBooks.run(io);


        assertEquals(
            output.get(0), "foo"
        );




    }
}