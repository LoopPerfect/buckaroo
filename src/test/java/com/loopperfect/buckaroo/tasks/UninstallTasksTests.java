package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
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
                RecipeIdentifier.of("org", "example"), AnySemanticVersion.of()
            )));

        Files.write(fs.getPath("buckaroo.json"), Serializers.serialize(project).getBytes(Charsets.UTF_8));

        UninstallTasks.uninstallInWorkingDirectory(
            fs,
            ImmutableList.of(PartialRecipeIdentifier.of(Identifier.of("example")))).toList().blockingGet();

        final Project newProject = Serializers.parseProject(EvenMoreFiles.read(fs.getPath("buckaroo.json")))
            .right().get();

        assertTrue(!newProject.dependencies.requires(RecipeIdentifier.of("org", "example")));
    }
}
