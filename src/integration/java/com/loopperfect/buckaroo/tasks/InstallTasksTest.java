package com.loopperfect.buckaroo.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.TestCase.assertTrue;

public final class InstallTasksTest {

    @Test
    public void installRecipe() throws Exception {

        final Context context = Context.of(Jimfs.newFileSystem(), Schedulers.newThread());

        final Recipe valuable = Recipe.of(
            "Valuable",
            "https://github.com/loopperfect/valuable",
            ImmutableMap.of(
                SemanticVersion.of(0, 1),
                RecipeVersion.of(
                    RemoteArchive.of(
                        new URL("https://github.com/loopperfect/valuable/archive/da6f41cab53eed9a8ee490a1d2c11c091bce540d.zip"),
                        HashCode.fromString("e65c42bdb93598727c0a6b965b5797a2e39ead076219daf737a4169d6ca560b7"),
                        "valuable-da6f41cab53eed9a8ee490a1d2c11c091bce540d"))));

        final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
            PartialDependency.of(
                Identifier.of("loopperfect"),
                Identifier.of("valuable"),
                AnySemanticVersion.of()));

        EvenMoreFiles.writeFile(context.fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "loopperfect", "valuable.json"),
            Serializers.serialize(valuable));

        InitTasks.initWorkingDirectory(context).toList().blockingGet();

        InstallTasks.installDependencyInWorkingDirectory(context.fs, partialDependencies).toList().blockingGet();

        assertTrue(Files.exists(context.fs.getPath("BUCKAROO_DEPS")));

        final Path dependencyFolder = context.fs.getPath(
            "buckaroo", "official", "loopperfect", "valuable");

        assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
        assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));
    }

    @Test
    public void installDirectlyFromGitHub1() throws Exception {

        final Context context = Context.of(Jimfs.newFileSystem(), Schedulers.newThread());

        final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
            PartialDependency.of(
                Identifier.of("github"),
                Identifier.of("njlr"),
                Identifier.of("test-lib-a"),
                AnySemanticVersion.of()));

        InitTasks.initWorkingDirectory(context).toList().blockingGet();

        InstallTasks.installDependencyInWorkingDirectory(context.fs, partialDependencies).toList().blockingGet();

        assertTrue(Files.exists(context.fs.getPath("BUCKAROO_DEPS")));

        final Path dependencyFolder = context.fs.getPath(
            "buckaroo", "github", "njlr", "test-lib-a");

        assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
        assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));
    }

    @Test
    public void installDirectlyFromGitHub2() throws Exception {

        final Context context = Context.of(Jimfs.newFileSystem(), Schedulers.newThread());

        final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
            PartialDependency.of(
                Identifier.of("github"),
                Identifier.of("njlr"),
                Identifier.of("test-lib-d"),
                AnySemanticVersion.of()));

        InitTasks.initWorkingDirectory(context).toList().blockingGet();

        InstallTasks.installDependencyInWorkingDirectory(context.fs, partialDependencies).toList().blockingGet();

        assertTrue(Files.exists(context.fs.getPath("BUCKAROO_DEPS")));

        final Path dependencyFolder = context.fs.getPath(
            "buckaroo", "github", "njlr", "test-lib-d");

        assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
        assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));
    }

    @Test
    public void installAThenInstallB() throws Exception {

        final Context context = Context.of(Jimfs.newFileSystem(), Schedulers.newThread());

        // Write the recipe files
        final Recipe valuable = Recipe.of(
            "Valuable",
            "https://github.com/loopperfect/valuable",
            ImmutableMap.of(
                SemanticVersion.of(0, 1),
                RecipeVersion.of(
                    RemoteArchive.of(
                        new URL("https://github.com/loopperfect/valuable/archive/da6f41cab53eed9a8ee490a1d2c11c091bce540d.zip"),
                        HashCode.fromString("e65c42bdb93598727c0a6b965b5797a2e39ead076219daf737a4169d6ca560b7"),
                        "valuable-da6f41cab53eed9a8ee490a1d2c11c091bce540d"))));

        final Recipe neither = Recipe.of(
            "Neither",
            "https://github.com/loopperfect/neither",
            ImmutableMap.of(
                SemanticVersion.of(0, 1),
                RecipeVersion.of(
                    RemoteArchive.of(
                        new URL("https://github.com/loopperfect/neither/archive/c313b3ce65249b2dfc3f7820e7ed4873111d2fe8.zip"),
                        HashCode.fromString("fe58cd55cd9177abc1ffe9550091b678ad265e5ead40305a53d5c55e562a9b68"),
                        "neither-c313b3ce65249b2dfc3f7820e7ed4873111d2fe8"))));

        EvenMoreFiles.writeFile(context.fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "loopperfect", "valuable.json"),
            Serializers.serialize(valuable));

        EvenMoreFiles.writeFile(context.fs.getPath(System.getProperty("user.home"),
            ".buckaroo", "buckaroo-recipes", "recipes", "loopperfect", "neither.json"),
            Serializers.serialize(neither));

        // Create a project file
        InitTasks.initWorkingDirectory(context).toList().blockingGet();

        assertTrue(Files.exists(context.fs.getPath("buckaroo.json")));

        // Install valuable
        {
            final ImmutableList<PartialDependency> partialDependencies = ImmutableList.of(
                PartialDependency.of(
                    Identifier.of("loopperfect"),
                    Identifier.of("valuable"),
                    AnySemanticVersion.of()));

            InstallTasks.installDependencyInWorkingDirectory(context.fs, partialDependencies)
                .toList()
                .blockingGet();

            final Path dependencyFolder = context.fs.getPath(
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
                    AnySemanticVersion.of()));

            InstallTasks.installDependencyInWorkingDirectory(context.fs, partialDependencies)
                .toList()
                .blockingGet();

            final Path dependencyFolder = context.fs.getPath(
                "buckaroo", "official", "loopperfect", "neither");

            assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
            assertTrue(Files.exists(dependencyFolder.resolve("BUCKAROO_DEPS")));
        }

        assertTrue(Files.exists(context.fs.getPath("BUCKAROO_DEPS")));
    }
}
