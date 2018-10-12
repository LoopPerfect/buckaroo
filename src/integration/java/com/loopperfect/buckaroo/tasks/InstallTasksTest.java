package com.loopperfect.buckaroo.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.io.MoreFiles;
import com.google.common.jimfs.Jimfs;
import com.google.common.util.concurrent.SettableFuture;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.versioning.WildcardVersion;
import org.junit.Assert;
import org.junit.Test;
import sun.reflect.generics.tree.Wildcard;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;

public final class InstallTasksTest {

    @Test
    public void installRecipe() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Recipe valuable = Recipe.of(
            "Valuable",
            new URI("https://github.com/loopperfect/valuable"),
            ImmutableMap.of(
                SemanticVersion.of(0, 1),
                RecipeVersion.of(
                    RemoteArchive.of(
                        new URI("https://github.com/loopperfect/valuable/archive/da6f41cab53eed9a8ee490a1d2c11c091bce540d.zip"),
                        HashCode.fromString("e65c42bdb93598727c0a6b965b5797a2e39ead076219daf737a4169d6ca560b7"),
                        "valuable-da6f41cab53eed9a8ee490a1d2c11c091bce540d"))));

        final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
            PartialDependency.of(
                Identifier.of("loopperfect"),
                Identifier.of("valuable"),
                WildcardVersion.of()));

        EvenMoreFiles.writeFile(fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "loopperfect", "valuable.json"),
            Serializers.serialize(valuable));

        final CountDownLatch latch1 = new CountDownLatch(1);

        InitTasks.initWorkingDirectory(fs).subscribe(next -> {

        }, error -> {

        }, latch1::countDown);

        latch1.await(5000L, TimeUnit.MILLISECONDS);

        final CountDownLatch latch2 = new CountDownLatch(1);

        InstallTasks.installDependencyInWorkingDirectory(fs, partialDependencies).subscribe(next -> {

        }, error -> {

        }, latch2::countDown);

        latch2.await(5000L, TimeUnit.MILLISECONDS);

        assertTrue(Files.exists(fs.getPath("BUCKAROO_DEPS")));

        final Path dependencyFolder = fs.getPath(
            "buckaroo", "official", "loopperfect", "valuable");

        assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
        assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));

//        TestUtils.printTree(fs.getPath("/"));

        assertTrue(!Files.exists(dependencyFolder.getParent().resolve("valuable.zip")));
    }

    @Test
    public void installDirectlyFromGitHub1() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        // Workaround: JimFs does not implement .toFile;
        // We clone and fail buckaroo-recipes if it does not exist, so we create it.
        MoreFiles.createParentDirectories(fs.getPath(
            System.getProperty("user.home"),
            ".buckaroo",
            "buckaroo-recipes",
            ".git"));

        final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
            PartialDependency.of(
                Identifier.of("github"),
                Identifier.of("njlr"),
                Identifier.of("test-lib-a"),
                WildcardVersion.of()));

        InitTasks.initWorkingDirectory(fs).toList().blockingGet();

        final List<Event> events = InstallTasks.installDependencyInWorkingDirectory(fs, partialDependencies).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("BUCKAROO_DEPS")));

        final Path dependencyFolder = fs.getPath(
            "buckaroo", "github", "njlr", "test-lib-a");

        assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
        assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));
    }

    @Test
    public void installDirectlyFromGitHub2() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        // Workaround: JimFs does not implement .toFile;
        // We clone and fail buckaroo-recipes if it does not exist, so we create it.
        MoreFiles.createParentDirectories(fs.getPath(
            System.getProperty("user.home"),
            ".buckaroo",
            "buckaroo-recipes",
            ".git"));

        final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
            PartialDependency.of(
                Identifier.of("github"),
                Identifier.of("njlr"),
                Identifier.of("test-lib-d"),
                WildcardVersion.of()));

        InitTasks.initWorkingDirectory(fs).toList().blockingGet();

        InstallTasks.installDependencyInWorkingDirectory(fs, partialDependencies).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("BUCKAROO_DEPS")));

        final Path dependencyFolder = fs.getPath(
            "buckaroo", "github", "njlr", "test-lib-d");

        assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
        assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));
    }

    @Test
    public void installAThenInstallB() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        // Write the recipe files
        final Recipe valuable = Recipe.of(
            "Valuable",
            new URI("https://github.com/loopperfect/valuable"),
            ImmutableMap.of(
                SemanticVersion.of(0, 1),
                RecipeVersion.of(
                    RemoteArchive.of(
                        new URI("https://github.com/loopperfect/valuable/archive/da6f41cab53eed9a8ee490a1d2c11c091bce540d.zip"),
                        HashCode.fromString("e65c42bdb93598727c0a6b965b5797a2e39ead076219daf737a4169d6ca560b7"),
                        "valuable-da6f41cab53eed9a8ee490a1d2c11c091bce540d"))));

        final Recipe neither = Recipe.of(
            "Neither",
            new URI("https://github.com/loopperfect/neither"),
            ImmutableMap.of(
                SemanticVersion.of(0, 1),
                RecipeVersion.of(
                    RemoteArchive.of(
                        new URI("https://github.com/loopperfect/neither/archive/c313b3ce65249b2dfc3f7820e7ed4873111d2fe8.zip"),
                        HashCode.fromString("fe58cd55cd9177abc1ffe9550091b678ad265e5ead40305a53d5c55e562a9b68"),
                        "neither-c313b3ce65249b2dfc3f7820e7ed4873111d2fe8"))));

        EvenMoreFiles.writeFile(fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "loopperfect", "valuable.json"),
            Serializers.serialize(valuable));

        EvenMoreFiles.writeFile(fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "loopperfect", "neither.json"),
            Serializers.serialize(neither));

        // Create a project file
        InitTasks.initWorkingDirectory(fs).toList().blockingGet();

        assertTrue(Files.exists(fs.getPath("buckaroo.json")));

        // Install valuable
        {
            final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
                PartialDependency.of(
                    Identifier.of("loopperfect"),
                    Identifier.of("valuable"),
                    WildcardVersion.of()));

            InstallTasks.installDependencyInWorkingDirectory(fs, partialDependencies)
                .toList()
                .blockingGet();

            final Path dependencyFolder = fs.getPath(
                "buckaroo", "official", "loopperfect", "valuable");

            assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
            assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));
        }

        // Install neither
        {
            final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
                PartialDependency.of(
                    Identifier.of("loopperfect"),
                    Identifier.of("neither"),
                    WildcardVersion.of()));

            InstallTasks.installDependencyInWorkingDirectory(fs, partialDependencies)
                .toList()
                .blockingGet();

            final Path dependencyFolder = fs.getPath(
                "buckaroo", "official", "loopperfect", "neither");

            assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
            assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));
        }

        assertTrue(Files.exists(fs.getPath("BUCKAROO_DEPS")));
    }

    @Test
    public void failsGracefullyForIncorrectHashes() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        EvenMoreFiles.writeFile(fs.getPath("buckaroo.json"), Serializers.serialize(Project.of("test")));

        final Recipe recipe = Recipe.of(
            "Valuable",
            new URI("http://www.example.com"),
            ImmutableMap.of(
                SemanticVersion.of(1),
                RecipeVersion.of(
                    RemoteArchive.of(
                        new URI("https://github.com/LoopPerfect/valuable/archive/v0.1.0.zip"),
                        HashCode.fromString("aaaaaaaaaaaaaaaaaaaaaaa71dd4a1fd3400f1d04b84954beb2f514ec69934c0"),
                        "valuable-0.1.0"))));

        EvenMoreFiles.writeFile(
            fs.getPath(System.getProperty("user.home"),
                ".buckaroo", "buckaroo-recipes", "recipes", "loopperfect", "valuable.json"),
            Serializers.serialize(recipe));

        final ImmutableList<PartialDependency> toInstall = ImmutableList.of(
            PartialDependency.of(Identifier.of("loopperfect"), Identifier.of("valuable")));

        final SettableFuture<Throwable> futureException = SettableFuture.create();

        InstallTasks.installDependencyInWorkingDirectory(fs, toInstall)
            .subscribe(
                next -> {

                }, error -> {
                    futureException.set(error);
                }, () -> {

                });

        final Throwable exception = futureException.get(5000L, TimeUnit.MILLISECONDS);

        assertTrue(exception instanceof DownloadFileException);
        assertTrue(exception.getCause() instanceof HashMismatchException);
    }
}
