package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.MoreFiles;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.versioning.WildcardVersion;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

public final class UninstallTasksTests {

    @Test
    public void uninstallRemovesTheDependencyFromTheProjectFile() throws Exception {

        final FileSystem fs = Jimfs.newFileSystem();

        final Project project = Project.of(
            "My Project",
            DependencyGroup.of(ImmutableMap.of(
                RecipeIdentifier.of("org", "example"), WildcardVersion.of()
            )));

        Files.write(fs.getPath("buckaroo.json"), Serializers.serialize(project).getBytes(Charsets.UTF_8));

        // Workaround: JimFs does not implement .toFile;
        // We clone and fail buckaroo-recipes if it does not exist, so we create it.
        MoreFiles.createParentDirectories(fs.getPath(
            System.getProperty("user.home"),
            ".buckaroo",
            "buckaroo-recipes",
            ".git"));

        UninstallTasks.uninstallInWorkingDirectory(
            fs,
            ImmutableList.of(PartialRecipeIdentifier.of(Identifier.of("example")))).toList().blockingGet();

        final Project newProject = Serializers.parseProject(EvenMoreFiles.read(fs.getPath("buckaroo.json")))
            .right().get();

        assertTrue(!newProject.dependencies.requires(RecipeIdentifier.of("org", "example")));
    }
}
