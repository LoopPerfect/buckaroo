package com.loopperfect.buckaroo.routines;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IOContext;
import com.loopperfect.buckaroo.serialization.Serializers;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RoutinesTest {

    @Test
    public void loadConfigReadFileFailed() throws Exception {

        final IOContext io = IOContext.fake();

        final Path path = io.fs().getPath(
            io.fs().homeDirectory().toString(),
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
                io.fs().homeDirectory(),
                ".buckaroo",
                "config.json");

        io.fs().createDirectory(path.getParent().toString());
        io.fs().writeFile(path.toString(), "");

        assertTrue(Routines.readConfig(path.toString()).run(io).join(x -> true, x -> false));
    }

    @Test
    public void loadConfigParsesEmptyCookbook() throws Exception {

        final IOContext io = IOContext.fake();

        final Path path = io.fs().getPath(
                io.fs().homeDirectory(),
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

    @Test
    public void testReadCookBooks() {

        final IOContext io = IOContext.fake();

        final String buckarooDirectory = io.fs().getPath(
            io.fs().homeDirectory(),
            ".buckaroo").toString();

        final String configPath = io.fs().getPath(
            buckarooDirectory,
            "config.json").toString();

        final String configContent = "{" +
            "\"cookBooks\":[\n" +
            "    {" +
            "      \"name\": \"buckaroo-official\", " +
            "      \"url\": \"git@github.com:njlr/buckaroo-official.git\"" +
            "    }" +
            "  ]" +
            "}";

        io.fs().createDirectory(buckarooDirectory);
        io.fs().writeFile(configPath, configContent);

        final String buckarooOfficialPath = io.fs().getPath(buckarooDirectory, "buckaroo-official").toString();

        io.fs().createDirectory(buckarooOfficialPath);

        io.fs().writeFile(
            io.fs().getPath(buckarooOfficialPath, "recipes", "org.json").toString(),
            "{ \"name\": \"Org\" }");

        io.fs().createDirectory(io.fs().getPath(buckarooOfficialPath, "recipes", "org").toString());

        final Recipe magicRecipe = Recipe.of("Magic", "magic.com", ImmutableMap.of());

        io.fs().writeFile(
            io.fs().getPath(buckarooOfficialPath, "recipes", "org", "magic.json").toString(),
            Serializers.serialize(magicRecipe));

        final Either<IOException, BuckarooConfig> config = Routines.readConfig(configPath).run(io);

        assertTrue(config.join(l -> false, r -> true));

        final Either<IOException, ImmutableList<CookBook>> cookBooks =
            Routines.readCookBooks(config.right().get()).run(io);

        assertTrue(cookBooks.join(l -> false, r -> true));
    }
}
