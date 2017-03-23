package com.loopperfect.buckaroo.cli;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.SemanticVersionRequirement;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Install;
import com.loopperfect.buckaroo.routines.Routines;

import java.util.Objects;
import java.util.Optional;

public final class InstallCommand implements CLICommand {

    public final RecipeIdentifier project;
    public final Optional<SemanticVersionRequirement> versionRequirement;

    private InstallCommand(final RecipeIdentifier project, final Optional<SemanticVersionRequirement> versionRequirement) {
        this.project = Preconditions.checkNotNull(project);
        this.versionRequirement = Preconditions.checkNotNull(versionRequirement);
    }

    @Override
    public IO<Unit> routine() {
        return Install.routine(project, versionRequirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, versionRequirement);
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

        return Objects.equals(project, other.project) &&
            Objects.equals(versionRequirement, other.versionRequirement);
    }

    public static InstallCommand of(final RecipeIdentifier project, final Optional<SemanticVersionRequirement> versionRequirement) {
        return new InstallCommand(project, versionRequirement);
    }

    public static InstallCommand of(final RecipeIdentifier project, final SemanticVersionRequirement versionRequirement) {
        return new InstallCommand(project, Optional.of(versionRequirement));
    }

    public static InstallCommand of(final RecipeIdentifier project) {
        return new InstallCommand(project, Optional.empty());
    }
}
