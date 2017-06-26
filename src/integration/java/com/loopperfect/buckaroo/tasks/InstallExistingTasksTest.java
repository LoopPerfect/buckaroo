package com.loopperfect.buckaroo.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.serialization.Serializers;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public final class InstallExistingTasksTest {

    @Test
    public void worksForExistingLockFile() throws Exception {

        final Context context = Context.of(Jimfs.newFileSystem(), Schedulers.newThread());

        final DependencyLocks locks = DependencyLocks.of(ImmutableList.of(
            DependencyLock.of(
                RecipeIdentifier.of("loopperfect", "valuable"),
                ResolvedDependency.of(Either.right(
                    RemoteArchive.of(
                        new URL("https://github.com/LoopPerfect/valuable/archive/v0.1.0.zip"),
                        HashCode.fromString("639d7d0df95f8467f4aa8da71dd4a1fd3400f1d04b84954beb2f514ec69934c0"),
                        "valuable-0.1.0"))))));

        EvenMoreFiles.writeFile(context.fs.getPath("buckaroo.json"), Serializers.serialize(Project.of()));
        EvenMoreFiles.writeFile(context.fs.getPath("buckaroo.lock.json"), Serializers.serialize(locks));

        InstallExistingTasks.installExistingDependenciesInWorkingDirectory(context).toList().blockingGet();

        assertTrue(Files.exists(context.fs.getPath(".buckconfig")));
        assertTrue(Files.exists(context.fs.getPath(".buckconfig.local")));

        final Path dependencyFolder = context.fs.getPath(
            "buckaroo", "official", "loopperfect", "valuable");

        assertTrue(Files.exists(dependencyFolder.resolve(".buckconfig")));
        assertTrue(Files.exists(dependencyFolder.resolve(".buckconfig.local")));
        assertTrue(Files.exists(dependencyFolder.resolve("BUCK")));
    }

    @Test
    public void worksForExistingLockFile2() throws Exception {

        final Context context = Context.of(Jimfs.newFileSystem(), Schedulers.newThread());

        final DependencyLocks locks = DependencyLocks.of(ImmutableList.of(
            DependencyLock.of(
                RecipeIdentifier.of("loopperfect", "valuable"),
                ResolvedDependency.of(Either.right(
                    RemoteArchive.of(
                        new URL("https://github.com/LoopPerfect/valuable/archive/v0.1.0.zip"),
                        HashCode.fromString("639d7d0df95f8467f4aa8da71dd4a1fd3400f1d04b84954beb2f514ec69934c0"),
                        "valuable-0.1.0"))))));

        EvenMoreFiles.writeFile(context.fs.getPath("buckaroo.json"), Serializers.serialize(Project.of()));
        EvenMoreFiles.writeFile(context.fs.getPath("buckaroo.lock.json"), Serializers.serialize(locks));

        InstallExistingTasks.installExistingDependenciesInWorkingDirectory(context).toList().blockingGet();

        assertTrue(Files.exists(context.fs.getPath("buckaroo", "official", "loopperfect", "valuable", "BUCK")));

        final DependencyLocks locks2 = DependencyLocks.of(ImmutableList.of(

            DependencyLock.of(
                RecipeIdentifier.of("loopperfect", "neither"),
                ResolvedDependency.of(Either.right(
                    RemoteArchive.of(
                        new URL("https://github.com/LoopPerfect/neither/archive/v0.0.1.zip"),
                        HashCode.fromString("f88bd7e72e509bead972fa4d93f6a9791e85c7d8b322a636e81df84a28d56fe1"),
                        "neither-0.0.1")))),

            DependencyLock.of(
                RecipeIdentifier.of("loopperfect", "valuable"),
                ResolvedDependency.of(Either.right(
                    RemoteArchive.of(
                        new URL("https://github.com/LoopPerfect/valuable/archive/v0.1.0.zip"),
                        HashCode.fromString("639d7d0df95f8467f4aa8da71dd4a1fd3400f1d04b84954beb2f514ec69934c0"),
                        "valuable-0.1.0"))))

        ));

        Files.delete( context.fs.getPath("buckaroo.lock.json") );
        EvenMoreFiles.writeFile(context.fs.getPath("buckaroo.lock.json"), Serializers.serialize(locks2));

        InstallExistingTasks.installExistingDependenciesInWorkingDirectory(context).toList().blockingGet();

        assertTrue(Files.exists(context.fs.getPath("buckaroo", "official", "loopperfect", "valuable", "BUCK")));
        assertTrue(Files.exists(context.fs.getPath("buckaroo", "official", "loopperfect", "neither", "BUCK")));
    }
}