package com.loopperfect.buckaroo.cli;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.PartialDependency;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.tasks.InstallTasks;

import java.nio.file.FileSystem;
import java.util.Collection;
import java.util.Objects;

public final class InstallCommand implements CLICommand {

    public final ImmutableList<PartialDependency> dependencies;

    private InstallCommand(final ImmutableList<PartialDependency> dependencies) {
        this.dependencies = Preconditions.checkNotNull(dependencies);
    }

    @Override
    public IO<Unit> routine() {
        return context -> {
            final FileSystem fs = context.fs().fileSystem();
            context.console().println("[" + String.join(
                ", ", dependencies.stream().map(PartialDependency::encode).collect(ImmutableList.toImmutableList())) + "]");
            InstallTasks.installDependencyInWorkingDirectory(fs, dependencies).subscribe(
                next -> {
                    System.out.println(next);
                },
                error -> {
                    error.printStackTrace();
                },
                () -> {
                    System.out.println("Done.");
                });
            return Unit.of();
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencies);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof InstallCommand)) {
            return false;
        }

        final InstallCommand other = (InstallCommand) obj;

        return Objects.equals(dependencies, other.dependencies);
    }

    public static InstallCommand of(final Collection<PartialDependency> dependencies) {
        return new InstallCommand(ImmutableList.copyOf(dependencies));
    }

    public static InstallCommand of(final PartialDependency... dependencies) {
        return new InstallCommand(ImmutableList.copyOf(dependencies));
    }
}
